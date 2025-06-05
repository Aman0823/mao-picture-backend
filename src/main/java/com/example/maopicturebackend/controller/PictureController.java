package com.example.maopicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.maopicturebackend.annotation.AuthCheck;
import com.example.maopicturebackend.common.BaseResponse;
import com.example.maopicturebackend.common.DeleteRequest;
import com.example.maopicturebackend.common.ResultUtils;
import com.example.maopicturebackend.constant.UserConstant;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.picture.*;
import com.example.maopicturebackend.model.entity.Picture;
import com.example.maopicturebackend.model.entity.PictureTagCategory;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.enums.PictureReviewStatusEnum;
import com.example.maopicturebackend.model.vo.PictureVO;
import com.example.maopicturebackend.service.PictureService;
import com.example.maopicturebackend.service.SpaceService;
import com.example.maopicturebackend.service.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@RestController
@Slf4j
@Api(tags = "图片模块接口")
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceService spaceService;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
//                    缓存五分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    @PostMapping("/upload")
    @ApiOperation(value = "用户上传图片")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadDTO pictureUploadDTO,
                                                 HttpServletRequest request) {
        User userInfo = userService.getUserInfo(request);
        ThrowUtils.throwIf(userInfo == null, ErrorCode.NOT_LOGIN_ERROR);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadDTO, userInfo);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getUserInfo(request);
        long id = deleteRequest.getId();
        pictureService.deletePic(id, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateDTO pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
//        权限校验
        User user = userService.getUserInfo(request);
        pictureService.fillReviewParams(picture, user);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User user = userService.getUserInfo(request);
            pictureService.checkPicAuth(user, picture);
        }
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryDTO pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryDTO pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
//        空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
//        公开图库
        if (spaceId == null) {
//            普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        }else{
//            私有空间
            User user = userService.getUserInfo(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间权限不存在");
            if (!user.getId().equals(space.getUserId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
            }
        }
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditDTO pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getUserInfo(request);
        pictureService.editPic(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 管理员审核图片
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> doPictureReview(@RequestBody PictureReviewDTO pictureReviewDTO,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewDTO == null, ErrorCode.PARAMS_ERROR);

        User user = userService.getUserInfo(request);
        pictureService.doPictureReview(pictureReviewDTO, user);
        return ResultUtils.success("ok");

    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadDTO pictureUploadDTO,
            HttpServletRequest request) {
        User loginUser = userService.getUserInfo(request);
        String fileUrl = pictureUploadDTO.getFileUrl();

        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadDTO, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页查询缓存中的图片列表
     *
     * @param pictureQueryDTO
     * @param request
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryDTO pictureQueryDTO, HttpServletRequest request) {
        long current = pictureQueryDTO.getCurrent();
        long size = pictureQueryDTO.getPageSize();
//        限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        普通用户默认智能查看已过审的图片
        pictureQueryDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
//        构建缓存key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryDTO);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        1.从本地缓存中获取
        String cacheKey = "listPictureVOByPage:" + hashKey;
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
//        如果命中本地缓存
        if (cachedValue != null) {
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(pictureVOPage);
        }
//        如果没有命中本地缓存
//        2.从redis中查询
        String redisKey = "maoPicture:listPictureByPage:" + hashKey;
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        cachedValue = valueOperations.get(redisKey);
        if (cachedValue != null) {
//            如果缓存命中
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
//        查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryDTO));
//        获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//        更新缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
//        更新本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
//        存入redis,5-10分钟随机过期时间，防止雪崩
        valueOperations.set(redisKey, JSONUtil.toJsonStr(pictureVOPage), 300 + RandomUtil.randomInt(0, 300), TimeUnit.SECONDS);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 批量抓取图片
     *
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchDTO pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getUserInfo(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }


}
