package com.example.maopicturebackend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//异常处理工具类
@Slf4j
public class ThrowUtils {
    public static void throwIf(boolean condition,RuntimeException runtimeException){
        if (condition){
            throw runtimeException;
        }
    }
    public static void throwIf(boolean condition,ErrorCode errorCode){

        throwIf(condition,new BusinessException(errorCode));
    }
    public static void throwIf(boolean condition,ErrorCode errorCode,String message){
        throwIf(condition,new BusinessException(errorCode,message));
    }
}
