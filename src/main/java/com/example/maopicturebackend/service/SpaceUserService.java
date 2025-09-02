package com.example.maopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.maopicturebackend.model.dto.space.spaceUser.SpaceUserAddDTO;
import com.example.maopicturebackend.model.dto.space.spaceUser.SpaceUserQueryDTO;
import com.example.maopicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.maopicturebackend.model.vo.space.spaceUser.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author mao
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-06-16 17:54:19
 */
public interface SpaceUserService extends IService<SpaceUser> {
    long addSpaceUser(SpaceUserAddDTO spaceUserAddDTO);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDTO spaceUserQueryDTO);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

}
