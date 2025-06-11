package com.example.maopicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureDTO implements Serializable {

    private static final long serialVersionUID = -1490993893162976628L;
    /**
     * 空间ID
     */
    private Long pictureId;


}
