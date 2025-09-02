package com.example.maopicturebackend.controller;

import com.example.maopicturebackend.common.BaseResponse;
import com.example.maopicturebackend.common.DeleteRequest;
import com.example.maopicturebackend.common.ResultUtils;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.space.spaceUser.SpaceUserAddDTO;
import com.example.maopicturebackend.model.dto.space.spaceUser.SpaceUserEditDTO;
import com.example.maopicturebackend.model.dto.space.spaceUser.SpaceUserQueryDTO;
import com.example.maopicturebackend.model.entity.SpaceUser;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.vo.space.spaceUser.SpaceUserVO;
import com.example.maopicturebackend.service.SpaceUserService;
import com.example.maopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;

    /**
     * 新增空间用户
     *
     * @param spaceUserAddDTO
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddDTO spaceUserAddDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddDTO == null, ErrorCode.PARAMS_ERROR);
        userService.getUserInfo(request);
        long l = spaceUserService.addSpaceUser(spaceUserAddDTO);
        return ResultUtils.success(l);
    }

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> delSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getUserInfo(request);
        Long id = deleteRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
//        操作数据库
        boolean remove = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!remove, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(remove);
    }

    /**
     * 修改成员信息
     *
     * @param spaceUserEditDTO
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditDTO spaceUserEditDTO, HttpServletRequest request) {
        if (spaceUserEditDTO == null || spaceUserEditDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditDTO, spaceUser);
        spaceUserService.validSpaceUser(spaceUser, false);

        Long id = spaceUserEditDTO.getId();
        SpaceUser old = spaceUserService.getById(id);
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR);
//        操作数据库
        boolean updated = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(updated);
    }

    /**
     * 查询空间成员列表
     *
     * @param spaceUserQueryDTO
     * @param request
     * @return
     */
    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVO>> listSpaceUserVO(@RequestBody SpaceUserQueryDTO spaceUserQueryDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryDTO == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> list = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryDTO));
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(list);
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 获取我加入的空间列表
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUser>> listMyAdded(HttpServletRequest request) {
        User user = userService.getUserInfo(request);
        SpaceUserQueryDTO spaceUserQueryDTO = new SpaceUserQueryDTO();
        spaceUserQueryDTO.setUserId(user.getId());
        List<SpaceUser> list = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryDTO));
        return ResultUtils.success(list);
    }
}
