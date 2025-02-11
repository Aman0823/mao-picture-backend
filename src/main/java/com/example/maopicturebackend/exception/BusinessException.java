package com.example.maopicturebackend.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
//    错误码
    private final int errorCode;
    public BusinessException(int code, String errorMessage){
        super(errorMessage);
        this.errorCode = code;
    }
    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
    }
    public BusinessException(ErrorCode errorCode,String message){
        super(message);
        this.errorCode = errorCode.getCode();
    }
}
