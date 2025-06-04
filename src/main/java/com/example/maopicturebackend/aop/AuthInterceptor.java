package com.example.maopicturebackend.aop;

import com.example.maopicturebackend.annotation.AuthCheck;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.enums.UserRoleEnum;
import com.example.maopicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    /**
     * 执行拦截
     * @param point
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint point,AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User user = userService.getUserInfo(request);
        UserRoleEnum enumByValue = UserRoleEnum.getEnumByValue(mustRole);
        if (enumByValue == null){
            return point.proceed();
        }
//        必须有权限才会通过
        UserRoleEnum role = UserRoleEnum.getEnumByValue(user.getUserRole());
        ThrowUtils.throwIf(role==null,ErrorCode.NOT_LOGIN_ERROR);
//        必须有管理员权限才能通过
        ThrowUtils.throwIf(role != UserRoleEnum.ADMIN,ErrorCode.NO_AUTH_ERROR);
//        最后放行
        return point.proceed();


    }
}
