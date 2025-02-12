package com.example.maopicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginDTO implements Serializable {

    private static final long serialVersionUID = -196806285249811844L;
    //    用户账户
    private String userAccount;
//    账户密码
    private String userPassword;

}
