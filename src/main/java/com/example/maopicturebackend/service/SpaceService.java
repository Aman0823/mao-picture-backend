package com.example.maopicturebackend.service;

import com.example.maopicturebackend.model.dto.space.SpaceAddDTO;
import com.example.maopicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.maopicturebackend.model.entity.User;

/**
* @author mao
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-06-04 10:36:53
*/
public interface SpaceService extends IService<Space> {
    /**
     * 数据校验
     * @param space
     * @param add
     */
    void validSpace(Space space,boolean add);

    /**
     * 自动填充数据
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     * @param spaceAddDTO
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddDTO spaceAddDTO, User loginUser);
}
