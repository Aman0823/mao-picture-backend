package com.example.maopicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterDTO implements Serializable {
    private static final long serialVersionUID = 2603918106901214302L;
    //    用户账户
    private String userAccount;
//    账户密码
    private String userPassword;
//    确认密码
    private String checkPassword;

}
