package com.example.maopicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.example.maopicturebackend.constant.UserConstant;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.example.maopicturebackend.model.entity.Picture;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.SpaceUser;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.enums.SpaceRoleEnum;
import com.example.maopicturebackend.model.enums.SpaceTypeEnum;
import com.example.maopicturebackend.service.PictureService;
import com.example.maopicturebackend.service.SpaceService;
import com.example.maopicturebackend.service.SpaceUserService;
import com.example.maopicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
//        判断loginType,对space类型进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
//        管理员权限，表示权限校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
//        获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
//        如果所有字段都为空
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
//        获取userId
        User user = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = user.getId();
//        优先从上下文获取spaceUser对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
//        如果有spaceUserId，肯定是团队空间
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
//            这里会导致管理员在四月开机没有权限，可以再查一次库处理
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
//        如果没有spaceUserId，尝试通过spaceId或pictureId获取space对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId==null){
            Long pictureId = authContext.getPictureId();
            if (pictureId==null){
                return ADMIN_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId,pictureId)
                    .select(Picture::getId,Picture::getSpaceId,Picture::getUserId)
                    .one();
            ThrowUtils.throwIf(picture==null,ErrorCode.NOT_FOUND_ERROR,"未找到图片信息");
            spaceId = picture.getSpaceId();
//            公共图片，仅本人和管理员可以操作
            if (spaceId==null){
                if (picture.getUserId().equals(userId) || userService.isAdmin(user)){
                    return ADMIN_PERMISSIONS;
                }else{
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
//        获取space对象
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"未找到空间对象");
//        根据空间类型判断权限
        if (space.getSpaceType()== SpaceTypeEnum.PRIVATE.getValue()){
            if (space.getUserId().equals(user.getId()) || userService.isAdmin(user)){
                return ADMIN_PERMISSIONS;
            }else {
                return new ArrayList<>();
            }
        }else{
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId,spaceId)
                    .eq(SpaceUser::getUserId,userId)
                    .one();
            if (spaceUser==null){
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

    }

    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true;
        }
//        获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                .map(field -> ReflectUtil.getFieldValue(object, field))
                .allMatch(ObjectUtil::isEmpty);
    }

    @Override
    public List<String> getRoleList(Object o, String s) {
        return new ArrayList<>();
    }

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 兼容 get 和 post 操作
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            String partUri = requestUri.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }
}
