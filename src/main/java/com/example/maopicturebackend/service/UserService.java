package com.example.maopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.maopicturebackend.model.dto.user.UserLoginDTO;
import com.example.maopicturebackend.model.dto.user.UserQueryDTO;
import com.example.maopicturebackend.model.dto.user.UserRegisterDTO;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.vo.UserLoginVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author mao
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-12 16:31:24
*/
public interface UserService extends IService<User> {
    long register(UserRegisterDTO userRegisterDTO);

    UserLoginVO login(UserLoginDTO userLoginDTO, HttpServletRequest request);
    User getUserInfo(HttpServletRequest request);
    String getEncryptPassword(String password);

    void logout(HttpServletRequest request);
    QueryWrapper<User> getQueryWrapper(UserQueryDTO userQueryDTO);
}
