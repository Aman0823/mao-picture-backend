package com.example.maopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.maopicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.example.maopicturebackend.api.aliyun.model.dto.CreatePictureOutPaintingTaskDTO;
import com.example.maopicturebackend.model.dto.picture.*;
import com.example.maopicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.vo.PictureVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author mao
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-02-13 21:19:38
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传文件
     * @param inputSource
     * @param pictureUploadDTO
     * @param user
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, User user);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryRequest);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    void validPicture(Picture picture);
    void doPictureReview(PictureReviewDTO pictureReviewDTO,User user);

    void fillReviewParams(Picture picture, User loginUser);
    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchDTO
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchDTO pictureUploadByBatchDTO,
            User loginUser
    );

    @Async
    void clearPictureFile(Picture oldPicture);

    void checkPicAuth(User user, Picture picture);

    void deletePic(long pictureId, User user);

    void editPic(PictureEditDTO pictureEditDTO, User user);

    List<PictureVO> searchPicByColor(Long spaceId, String picColor, User user);

    void editPicByBatch(PictureEditByBatchDTO pictureEditByBatchDTO,User user);

    void batchEditPicMetaData(PictureEditByBatchDTO pictureEditByBatchDTO,Long spaceId,Long userId);

    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskDTO createPictureOutPaintingTaskRequest, User loginUser);
}
