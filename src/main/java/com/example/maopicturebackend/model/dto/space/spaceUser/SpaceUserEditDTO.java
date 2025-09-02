package com.example.maopicturebackend.model.dto.space.spaceUser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserEditDTO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
