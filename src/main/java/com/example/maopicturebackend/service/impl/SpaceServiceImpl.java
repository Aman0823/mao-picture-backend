package com.example.maopicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.space.SpaceAddDTO;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.enums.SpaceLevelEnum;
import com.example.maopicturebackend.service.SpaceService;
import com.example.maopicturebackend.mapper.SpaceMapper;
import com.example.maopicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
* @author mao
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-06-04 10:36:53
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserService userService;

    /**
     * 校验空间是否合法
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space==null, ErrorCode.PARAMS_ERROR);
//        从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

        if(add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称不能为空");
            }
            if (spaceLevel==null){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间基本不能为空");
            }
        }
//        修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间级别不存在");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"空间名称过长");
        }
    }

    /**
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
//        根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null){
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 创建空间
     * @param spaceAddDTO
     * @return
     */
    private final Map<Long,Object> lockMap = new ConcurrentHashMap<>();
    @Override
    public long addSpace(SpaceAddDTO spaceAddDTO, User loginUser) {
//        实体类和dto转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddDTO,space);
//        默认值
        if (StrUtil.isBlank(spaceAddDTO.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddDTO.getSpaceLevel()==null){
            spaceAddDTO.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
//        填充数据
        this.fillSpaceBySpaceLevel(space);
//        数据校验
        this.validSpace(space,true);
        Long id = loginUser.getId();
        space.setUserId(id);
//        权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddDTO.getSpaceLevel() && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限创建指定级别的空间");
        }
//        针对用户进行加锁
//        String lock = String.valueOf(id).intern();常量池加锁，数据不会及时释放
//        使用本地锁
        Object lock = lockMap.computeIfAbsent(id,key -> new Object());
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, id)
                        .exists();
                ThrowUtils.throwIf(exists,ErrorCode.OPERATION_ERROR,"每个用户只能有一个私有空间");
//            写入数据库
                boolean res = this.save(space);
                ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR);
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }
}




