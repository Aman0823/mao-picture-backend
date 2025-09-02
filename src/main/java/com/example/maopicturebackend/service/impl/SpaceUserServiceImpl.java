package com.example.maopicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.space.spaceUser.SpaceUserAddDTO;
import com.example.maopicturebackend.model.dto.space.spaceUser.SpaceUserQueryDTO;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.SpaceUser;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.enums.SpaceRoleEnum;
import com.example.maopicturebackend.model.vo.UserVO;
import com.example.maopicturebackend.model.vo.space.SpaceVO;
import com.example.maopicturebackend.model.vo.space.spaceUser.SpaceUserVO;
import com.example.maopicturebackend.service.SpaceService;
import com.example.maopicturebackend.service.SpaceUserService;
import com.example.maopicturebackend.mapper.SpaceUserMapper;
import com.example.maopicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mao
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-06-16 17:54:19
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {
    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;

    /**
     * 添加空间成员
     *
     * @param spaceUserAddDTO
     * @return
     */
    @Override
    public long addSpaceUser(SpaceUserAddDTO spaceUserAddDTO) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddDTO, spaceUser);
//        校验参数
        validSpaceUser(spaceUser, true);
//        数据库操作
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 校验空间成员
     * 添加成员时 空间id 和 用户id 必填
     *
     * @param spaceUser
     * @param add
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            是否已经在空间内
            if (Objects.equals(spaceUser.getSpaceId(), space.getId())){
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"该成员已在空间内");
            }
        }
//        校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    /**
     * 获取封装对象
     * @param spaceUserQueryDTO
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDTO spaceUserQueryDTO) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryDTO == null) {
            return queryWrapper;
        }
        Long id = spaceUserQueryDTO.getId();
        Long spaceId = spaceUserQueryDTO.getSpaceId();
        Long userId = spaceUserQueryDTO.getUserId();
        String spaceRole = spaceUserQueryDTO.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id),"id",id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId),"spaceId",spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId),"userId",userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole),"spaceRole",spaceRole);
        return queryWrapper;
    }

    /**
     * 获取空间成员封装类
     * @param spaceUser
     * @param request
     * @return
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        Long userId = spaceUser.getUserId();
        if (userId != null && userId>0){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
//        关键查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId>0){
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space,request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 1. 收集需要关联查询的用户 ID 和空间 ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3. 填充 SpaceUserVO 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }



}




