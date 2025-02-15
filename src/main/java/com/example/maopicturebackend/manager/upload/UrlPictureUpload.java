package com.example.maopicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.example.maopicturebackend.exception.BusinessException;
import com.example.maopicturebackend.exception.ErrorCode;
import com.example.maopicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {  
    @Override  
    protected void validPicture(Object inputSource) {  
        String fileUrl = (String) inputSource;  
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        // ... 跟之前的校验逻辑保持一致
        //        1.校验是否为url
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法地址");
        }
//        2.校验url协议
        ThrowUtils.throwIf(fileUrl.startsWith("http://") || fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持HTTP和HTTPS协议");
//        3.验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
//        4.检查文件格式
            String type = response.header("Content-Type");
            final List<String> TYPE_LIST = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");
            ThrowUtils.throwIf(!TYPE_LIST.contains(type), ErrorCode.PARAMS_ERROR, "文件格式错误");

//        5.校验文件大小
            String size = response.header("Content-Length");
            ThrowUtils.throwIf(size==null,ErrorCode.PARAMS_ERROR);
            long parsed = Long.parseLong(size);
            ThrowUtils.throwIf(parsed > 1024 * 1024, ErrorCode.PARAMS_ERROR, "文件大小过大");
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }  
  
    @Override  
    protected String getOriginFilename(Object inputSource) {  
        String fileUrl = (String) inputSource;  
        // 从 URL 中提取文件名  
        return FileUtil.mainName(fileUrl);
    }  
  
    @Override  
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;  
        // 下载文件到临时目录  
        HttpUtil.downloadFile(fileUrl, file);
    }  
}
