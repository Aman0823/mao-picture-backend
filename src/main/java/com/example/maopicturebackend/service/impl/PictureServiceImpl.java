package com.example.maopicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.manager.CosManager;
import com.example.maopicturebackend.manager.upload.FilePictureUpload;
import com.example.maopicturebackend.manager.upload.PictureUploadTemplate;
import com.example.maopicturebackend.manager.upload.UrlPictureUpload;
import com.example.maopicturebackend.model.dto.picture.*;
import com.example.maopicturebackend.model.dto.file.UploadPictureResult;
import com.example.maopicturebackend.model.entity.Picture;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.enums.PictureReviewStatusEnum;
import com.example.maopicturebackend.model.vo.PictureVO;
import com.example.maopicturebackend.model.vo.UserVO;
import com.example.maopicturebackend.service.PictureService;
import com.example.maopicturebackend.mapper.PictureMapper;
import com.example.maopicturebackend.service.SpaceService;
import com.example.maopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author mao
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-13 21:19:38
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    private UserService userService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    private SpaceService spaceService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, User user) {
//        权限校验
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR);
//      校验空间是否存在
        Long spaceId = pictureUploadDTO.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传
            if (!user.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
//            校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
//            校验大小
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }

//        校验参数
        Long pictureId = pictureUploadDTO.getId();
//      如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
//            校验空间是否一致
//            没传spaceId,就用原有图片的spaceId
            if (spaceId == null) {
                spaceId = pictureUploadDTO.getSpaceId();
            } else {
//                传了spaceId
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id不一致");
                }
            }
        }

//        如果是上传
//        上传图片
        String uploadFilePrefix = String.format("public/%s", user.getId());
//        根据inputSource区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
//        上传到云
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadFilePrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbNailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadDTO != null && StrUtil.isNotBlank(pictureUploadDTO.getPicName())) {
            picName = pictureUploadDTO.getPicName();
        }
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(user.getId());


//        操作数据库
//        如果picture不为空
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
//        更新额度
        Long finalSpaceId = spaceId;
        if (finalSpaceId != null) {
            transactionTemplate.execute(status -> {
//            插入数据
                        boolean save = this.saveOrUpdate(picture);
                        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "图片上传失败");
                        boolean update = spaceService.lambdaUpdate()
                                .eq(Space::getId, finalSpaceId)
                                .setSql("totalSize = totalSize + " + picture.getPicSize())
                                .setSql("totalCount = totalCount + 1")
                                .update();
                        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
                        return picture;
                    }
            );
        }
        boolean save = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewId", reviewerId);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");

        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装,默认只能查看已过审的数据
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            pictureVO.setUser(userVO);
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 管理员图片审核
     *
     * @param pictureReviewDTO
     * @param user
     */
    @Override
    public void doPictureReview(PictureReviewDTO pictureReviewDTO, User user) {
//        参数校验
        ThrowUtils.throwIf(pictureReviewDTO == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewDTO.getId();
        Integer reviewStatus = pictureReviewDTO.getReviewStatus();
//        获取图片状态
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewDTO.getReviewMessage();
        if (pictureReviewStatusEnum == null || id == null || pictureReviewStatusEnum.equals(PictureReviewStatusEnum.REVIEWING)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        判断图片状态是否与请求的一样
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复审核");
        }
//        数据库操作
//        如果是待审核
        Picture newPicture = new Picture();
        BeanUtils.copyProperties(oldPicture, newPicture);
        newPicture.setReviewTime(new Date());
        newPicture.setReviewerId(user.getId());
        boolean isUpdate = this.updateById(newPicture);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR);

    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓图
     *
     * @param pictureUploadByBatchDTO
     * @param loginUser
     * @return
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO, User loginUser) {
        String searchText = pictureUploadByBatchDTO.getSearchText();
        Integer count = pictureUploadByBatchDTO.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多请求30条数据");
//        抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (Exception e) {
            log.error("获取页面失败");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = (Element) document.getElementsByClass("dgControl").first();
        if (div == null) {
            log.error("获取元素失败");
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            String namePrefix = pictureUploadByBatchDTO.getNamePrefix();
            if (StrUtil.isBlank(namePrefix)) {
                namePrefix = searchText;
            }
            // 上传图片
            PictureUploadDTO pictureUploadRequest = new PictureUploadDTO();
            if (StrUtil.isNotBlank(namePrefix)) {
                // 设置图片名称，序号连续递增
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }

        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbNailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    /**
     * 对空间图片权限校验
     *
     * @param user
     * @param picture
     */
    @Override
    public void checkPicAuth(User user, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
//            公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
//            私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(user.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 删除文件
     *
     * @param pictureId
     * @param user
     */
    @Override
    public void deletePic(long pictureId, User user) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
//        判断是否存在
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
//        权限校验
        checkPicAuth(user, picture);
        //        更新额度
        Long finalSpaceId = picture.getSpaceId();
        transactionTemplate.execute(status -> {
//        操作数据库
                    boolean remove = this.removeById(pictureId);
                    ThrowUtils.throwIf(!remove, ErrorCode.OPERATION_ERROR);
                    boolean delete = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize = totalSize - " + picture.getPicSize())
                            .setSql("totalCount = totalCount - 1")
                            .update();
                    ThrowUtils.throwIf(!delete, ErrorCode.OPERATION_ERROR, "额度更新失败");
                    return picture;
                }
        );

//        异步清理文件
        this.clearPictureFile(picture);
    }

    @Override
    public void editPic(PictureEditDTO pictureEditDTO, User user) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditDTO, picture);
//        将list转为字符串
        picture.setTags(JSONUtil.toJsonStr(pictureEditDTO.getTags()));
//        设置编辑时间
        picture.setEditTime(new Date());
//        数据校验
        this.validPicture(picture);
//        判断是覅存在
        Long id = pictureEditDTO.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        校验权限
        checkPicAuth(user, oldPicture);
//        自动填充参数
        this.fillReviewParams(picture, user);
//        操作数据库
        boolean b = this.updateById(picture);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
    }
}




