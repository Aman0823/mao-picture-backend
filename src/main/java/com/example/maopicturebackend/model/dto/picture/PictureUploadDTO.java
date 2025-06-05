package com.example.maopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadDTO implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;
    /**
     * 文件地址
     */
    private String fileUrl;
    /**
     * 名称前缀
     */
    private String namePrefix;
    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;


    private static final long serialVersionUID = 1L;
}
