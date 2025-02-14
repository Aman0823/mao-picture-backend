package com.example.maopicturebackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.example.maopicturebackend.constant.UserConstant;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.user.UserLoginDTO;
import com.example.maopicturebackend.model.dto.user.UserQueryDTO;
import com.example.maopicturebackend.model.dto.user.UserRegisterDTO;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.enums.UserRoleEnum;
import com.example.maopicturebackend.model.vo.UserLoginVO;
import com.example.maopicturebackend.service.UserService;
import com.example.maopicturebackend.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mao
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-02-12 16:31:24
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户注册
     * @param userRegisterDTO
     * @return
     */
    @Override
    public long register(UserRegisterDTO userRegisterDTO) {
//        1.校验参数
        if (StrUtil.hasBlank(userRegisterDTO.getUserAccount(),
                userRegisterDTO.getUserPassword(),
                userRegisterDTO.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        if (userRegisterDTO.getUserAccount().length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号长度过短");
        }
        if (userRegisterDTO.getUserPassword().length()<8 || userRegisterDTO.getCheckPassword().length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度不能小于8");
        }
        if (!userRegisterDTO.getCheckPassword().equals(userRegisterDTO.getUserPassword())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次密码输入不一致");
        }
//        2.检查用户名是否重复
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userRegisterDTO.getUserAccount());
        long count = this.baseMapper.selectCount(userQueryWrapper);
        if (count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户已注册");
        }
//        3.密码加密
        String newPassword = getEncryptPassword(userRegisterDTO.getUserPassword());

//        4.数据库插入数据
        User user = new User();
        user.setUserAccount(userRegisterDTO.getUserAccount());
        user.setUserPassword(newPassword);
        user.setUserName("默认用户名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        if (!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统错误");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String password){
        String salt = "snionefvowengfvowierf";
        return DigestUtils.md5DigestAsHex((salt+password).getBytes());
    }

    /**
     * 用户登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO, HttpServletRequest request) {
//        校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userLoginDTO.getUserAccount(),userLoginDTO.getUserPassword()),ErrorCode.PARAMS_ERROR,"账号或密码为空");

//        用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        String toEncrypt = getEncryptPassword(userLoginDTO.getUserPassword());
        queryWrapper.eq("userAccount",userLoginDTO.getUserAccount());
        queryWrapper.eq("userPassword",toEncrypt);
        User user = this.baseMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user==null,ErrorCode.PARAMS_ERROR,"账号不存在或密码错误");
//        保存用户登录状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);
        UserLoginVO result = new UserLoginVO();
        BeanUtils.copyProperties(user,result);
        return result;
    }

    /**
     * 获取用户信息
     * @param request
     * @return
     */
    @Override
    public User getUserInfo(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        ThrowUtils.throwIf(userObj==null||user.getId()==null,ErrorCode.NOT_LOGIN_ERROR);
        Long userId = user.getId();
        user = this.getById(userId);
        ThrowUtils.throwIf(user==null,ErrorCode.NOT_LOGIN_ERROR);
        return user;
    }

    /**
     * 用户注销
     * @param request
     */
    @Override
    public void logout(HttpServletRequest request) {
//        判断是否登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(userObj==null,ErrorCode.OPERATION_ERROR,"未登录");
//        移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
    }

    /**
     * 把user对象转为查询条件
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryDTO userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 判断是否为管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        if (user==null) return false;
        return user.getUserRole().equals(UserRoleEnum.ADMIN.getValue());
    }




}




