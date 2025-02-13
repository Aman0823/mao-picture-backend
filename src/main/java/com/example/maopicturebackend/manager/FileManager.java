package com.example.maopicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.maopicturebackend.config.CosClientConfig;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import com.example.maopicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class FileManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private CosManager cosManager;

    /**
     * 上传文件
     * @param multipartFile
     * @param filepathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile,String filepathPrefix){
//        文件校验
        validatePicture(multipartFile);
//        文件名设置
        String uuid = RandomUtil.randomString(16);
        String originName = multipartFile.getOriginalFilename();
        String uploadName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originName));

//        上传文件，指定位置
        String uploadPath = String.format("/%s/%s", filepathPrefix, uploadName);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            // 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originName));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 删除临时文件
     * @param file
     */
    private void deleteTempFile(File file) {
        if (file!=null){
            boolean delete = file.delete();
            ThrowUtils.throwIf(!delete, ErrorCode.OPERATION_ERROR,"删除失败");

        }
    }

    /**
     * 校验图片
     * @param multipartFile
     */
    private void validatePicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile==null, ErrorCode.PARAMS_ERROR,"文件为空");
//        校验文件大小
        final long FILE_SIZE = 1024*1024;
        ThrowUtils.throwIf(multipartFile.getSize()>FILE_SIZE,ErrorCode.PARAMS_ERROR,"上传文件过大");
//        校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        List<String> FILE_ALLOWED_LIST = Arrays.asList("png","jpg","jpeg","webp");
        ThrowUtils.throwIf(!FILE_ALLOWED_LIST.contains(fileSuffix),ErrorCode.PARAMS_ERROR,"上传文件格式错误");


    }

}
