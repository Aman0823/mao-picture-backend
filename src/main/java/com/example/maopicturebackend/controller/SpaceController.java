package com.example.maopicturebackend.controller;

import com.example.maopicturebackend.annotation.AuthCheck;
import com.example.maopicturebackend.common.BaseResponse;
import com.example.maopicturebackend.common.DeleteRequest;
import com.example.maopicturebackend.common.ResultUtils;
import com.example.maopicturebackend.constant.UserConstant;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.space.SpaceAddDTO;
import com.example.maopicturebackend.model.dto.space.SpaceEditDTO;
import com.example.maopicturebackend.model.dto.space.SpaceLevel;
import com.example.maopicturebackend.model.dto.space.SpaceUpdateDTO;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.enums.SpaceLevelEnum;
import com.example.maopicturebackend.service.SpaceService;
import com.example.maopicturebackend.service.UserService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
@Api(tags = "空间模块接口")
@Slf4j
public class SpaceController {
    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;

    /**
     * 更新空间（管理员用）
     *
     * @param spaceUpdateDTO
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateDTO spaceUpdateDTO) {
        if (spaceUpdateDTO == null || spaceUpdateDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//      dto和实体类转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateDTO, space);
//        自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
//        数据校验
        spaceService.validSpace(space, false);
//        判断是否存在
        Long id = spaceUpdateDTO.getId();
        Space oldSpace = spaceService.getById(id);
        if (oldSpace == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
//        数据库
        boolean res = spaceService.updateById(space);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 创建空间
     *
     * @param spaceAddDTO
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddDTO spaceAddDTO, HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(spaceAddDTO == null, ErrorCode.PARAMS_ERROR);
        long addSpace = spaceService.addSpace(spaceAddDTO, loginUser);
        return ResultUtils.success(addSpace);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getUserInfo(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        Long id = deleteRequest.getId();
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        //        仅本人或管理员可编辑
        if (!space.getUserId().equals(user.getId()) || !userService.isAdmin(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean remove = spaceService.removeById(id);
        ThrowUtils.throwIf(!remove,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新空间（普通用户用）
     * @param spaceEditDTO
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditDTO spaceEditDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceEditDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getUserInfo(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
//        dto和实体类转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditDTO, space);
//        自动填充
        spaceService.fillSpaceBySpaceLevel(space);
//        数据校验
        spaceService.validSpace(space, false);
        Long id = spaceEditDTO.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
//        仅本人或管理员可编辑
        if (!oldSpace.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        操作数据库
        boolean update = spaceService.updateById(space);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }


}
