package com.example.maopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.maopicturebackend.model.dto.file.PictureQueryDTO;
import com.example.maopicturebackend.model.dto.file.UploadPictureResult;
import com.example.maopicturebackend.model.dto.user.PictureUploadDTO;
import com.example.maopicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author mao
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-02-13 21:19:38
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传文件
     * @param multipartFile
     * @param pictureUploadDTO
     * @param user
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadDTO pictureUploadDTO, User user);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryRequest);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);
}
