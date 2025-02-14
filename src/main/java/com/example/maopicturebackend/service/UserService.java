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
    /**
     * 注册
     * @param userRegisterDTO
     * @return
     */
    long register(UserRegisterDTO userRegisterDTO);

    /**
     * 登录
     * @param userLoginDTO
     * @param request
     * @return
     */
    UserLoginVO login(UserLoginDTO userLoginDTO, HttpServletRequest request);

    /**
     * 获取用户信息
     * @param request
     * @return
     */
    User getUserInfo(HttpServletRequest request);

    /**
     * 密码加密
     * @param password
     * @return
     */
    String getEncryptPassword(String password);

    /**
     * 用户登出
     * @param request
     */
    void logout(HttpServletRequest request);

    /**
     * 查询格式
     * @param userQueryDTO
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryDTO userQueryDTO);
    /**
     * 判断是否为管理员
     */
    boolean isAdmin(User user);

}
