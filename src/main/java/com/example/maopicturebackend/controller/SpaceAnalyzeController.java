package com.example.maopicturebackend.controller;

import com.example.maopicturebackend.common.BaseResponse;
import com.example.maopicturebackend.common.ResultUtils;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.space.analyze.*;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.vo.space.analyze.*;
import com.example.maopicturebackend.service.SpaceAnalyzeService;
import com.example.maopicturebackend.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {
    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;
    @Resource
    private UserService userService;

    /**
     * 获取空间使用状态
     *
     * @param spaceUsageAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeVO> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User userInfo = userService.getUserInfo(request);
        SpaceUsageAnalyzeVO spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeDTO, userInfo);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    /**
     * 根据分类获取分析情况
     *
     * @param spaceCategoryAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeVO>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getUserInfo(request);
        List<SpaceCategoryAnalyzeVO> spaceCategoryAnalyze = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeDTO, user);
        return ResultUtils.success(spaceCategoryAnalyze);
    }

    /**
     * 统计每个图片的标签数量
     *
     * @param spaceTagAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeVO>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getUserInfo(request);
        List<SpaceTagAnalyzeVO> result = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeDTO, user);
        return ResultUtils.success(result);
    }

    /**
     * 获取不同体积下的图片数据
     * @param spaceSizeAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeVO>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeDTO==null,ErrorCode.PARAMS_ERROR);
        User user = userService.getUserInfo(request);
        List<SpaceSizeAnalyzeVO> spaceSizeAnalyze = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeDTO, user);
        return ResultUtils.success(spaceSizeAnalyze);
    }

    /**
     * 时间维度统计用户上传行为
     * @param spaceUserAnalyzeDTO
     * @param request
     * @return
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeVO>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeDTO spaceUserAnalyzeDTO,HttpServletRequest request){
        ThrowUtils.throwIf(spaceUserAnalyzeDTO==null,ErrorCode.PARAMS_ERROR);
        User user = userService.getUserInfo(request);
        List<SpaceUserAnalyzeVO> spaceUserAnalyze = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeDTO, user);
        return ResultUtils.success(spaceUserAnalyze);
    }

    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeDTO spaceRankAnalyzeDTO,HttpServletRequest request){
        ThrowUtils.throwIf(spaceRankAnalyzeDTO==null,ErrorCode.PARAMS_ERROR);
        User user = userService.getUserInfo(request);
        List<Space> spaceRankAnalyze = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeDTO, user);
        return ResultUtils.success(spaceRankAnalyze);
    }
}
