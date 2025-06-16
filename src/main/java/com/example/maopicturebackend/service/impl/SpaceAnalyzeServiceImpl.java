package com.example.maopicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.space.analyze.*;
import com.example.maopicturebackend.model.entity.Picture;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.vo.space.analyze.*;
import com.example.maopicturebackend.service.PictureService;
import com.example.maopicturebackend.service.SpaceAnalyzeService;
import com.example.maopicturebackend.service.SpaceService;
import com.example.maopicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;

    /**
     * 校验空间分析权限
     *
     * @param spaceAnalyzeDTO
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeDTO spaceAnalyzeDTO, User loginUser) {
//        检查权限
        if (spaceAnalyzeDTO.isQueryAll() || spaceAnalyzeDTO.isQueryPublic()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权访问");
        } else {
//            私有空间权限校验
            Long spaceId = spaceAnalyzeDTO.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 根据查询范围填充对象
     *
     * @param spaceAnalyzeDTO 请求参数
     * @param queryWrapper    查询体
     */
    private static void fillAnalyzeQueryWrapper(SpaceAnalyzeDTO spaceAnalyzeDTO, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeDTO.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeDTO.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeDTO.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "非指定查询范围");
    }

    /**
     * 获取空间使用分析数据
     *
     * @param spaceUsageAnalyzeDTO 请求参数
     * @param user                 当前登录用户
     * @return 分析结果
     */
    @Override
    public SpaceUsageAnalyzeVO getSpaceUsageAnalyze(SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO, User user) {
        ThrowUtils.throwIf(spaceUsageAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        if (spaceUsageAnalyzeDTO.isQueryAll() || spaceUsageAnalyzeDTO.isQueryPublic()) {
//            查询全部或公共库逻辑
//            仅管理员可以访问
            boolean isAdmin = userService.isAdmin(user);
            ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR);
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            if (!spaceUsageAnalyzeDTO.isQueryAll()) {
                queryWrapper.isNull("spaceId");
            }
            List<Object> objectList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = objectList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            long usedCount = objectList.size();
//            封装返回结果
            SpaceUsageAnalyzeVO spaceUsageAnalyzeVO = new SpaceUsageAnalyzeVO();
            spaceUsageAnalyzeVO.setUsedCount(usedCount);
            spaceUsageAnalyzeVO.setUsedSize(usedSize);
//            公共图库无上限、无比例
            spaceUsageAnalyzeVO.setMaxSize(null);
            spaceUsageAnalyzeVO.setMaxCount(null);
            spaceUsageAnalyzeVO.setSizeUsageRatio(null);
            spaceUsageAnalyzeVO.setCountUsageRatio(null);
            return spaceUsageAnalyzeVO;
        } else {
//            查询指定空间
            ThrowUtils.throwIf(spaceUsageAnalyzeDTO.getSpaceId() == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceUsageAnalyzeDTO.getSpaceId());
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
//            权限校验
            spaceService.checkSpaceAuth(user, space);
//            构造返回结果
            SpaceUsageAnalyzeVO spaceUsageAnalyzeVO = new SpaceUsageAnalyzeVO();
            spaceUsageAnalyzeVO.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeVO.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeVO.setMaxCount(space.getMaxCount());
            spaceUsageAnalyzeVO.setMaxSize(space.getMaxSize());
//            算好百分比
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            spaceUsageAnalyzeVO.setSizeUsageRatio(sizeUsageRatio);
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeVO.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeVO;
        }
    }

    /**
     * 按照分类查询图片表的数据
     *
     * @param spaceCategoryAnalyzeDTO
     * @param user
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeVO> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO, User user) {
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeDTO, user);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeDTO, queryWrapper);
        queryWrapper.select("category AS category",
                        "COUNT(*) AS count",
                        "SUM(picSize) AS totalSize")
                .groupBy("category");
//        查询并转换结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeVO(category, count, totalSize);
                }).collect(Collectors.toList());
    }

    /**
     * 获取每个标签下的图片数量
     *
     * @param spaceTagAnalyzeDTO
     * @param user
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeVO> getSpaceTagAnalyze(SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, User user) {
        ThrowUtils.throwIf(spaceTagAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceTagAnalyzeDTO, user);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeDTO, queryWrapper);

        queryWrapper.select("tags");
        List<String> tagsList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

//        合并所有标签并统计使用次数
        Map<String, Long> tagCount = tagsList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

//        转化为响应对象，按使用次数降序排序
        return tagCount.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeVO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取不同体积大小的图片数
     *
     * @param spaceSizeAnalyzeDTO
     * @param user
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeVO> getSpaceSizeAnalyze(SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, User user) {
        ThrowUtils.throwIf(spaceSizeAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeDTO, user);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeDTO, queryWrapper);
        queryWrapper.select("picSize");
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());
//        定义分段范围，使用有序map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());

        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeVO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 时间维度统计用户上传行为
     * @param spaceUserAnalyzeDTO
     * @param user
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeVO> getSpaceUserAnalyze(SpaceUserAnalyzeDTO spaceUserAnalyzeDTO, User user) {
        checkSpaceAnalyzeAuth(spaceUserAnalyzeDTO, user);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeDTO.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeDTO, queryWrapper);

        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeDTO.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }

        // 分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询结果并转换
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeVO(period, count);
                })
                .collect(Collectors.toList());
    }


    /**
     * 按存储使用量排序查询前N个空间
     * @param spaceRankAnalyzeDTO
     * @param user
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeDTO spaceRankAnalyzeDTO, User user) {
        ThrowUtils.throwIf(!userService.isAdmin(user),ErrorCode.NO_AUTH_ERROR,"无权限查看空间排行");

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","spaceName","userId","totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT "+spaceRankAnalyzeDTO.getTopN());//取前N名

        return spaceService.list(queryWrapper);
    }
}
