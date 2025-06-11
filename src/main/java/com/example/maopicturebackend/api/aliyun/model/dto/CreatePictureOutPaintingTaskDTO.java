package com.example.maopicturebackend.api.aliyun.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskDTO implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskDTO.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
