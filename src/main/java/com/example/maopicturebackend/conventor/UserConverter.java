package com.example.maopicturebackend.conventor;

import com.example.maopicturebackend.common.Converter;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.vo.UserLoginVO;
import com.example.maopicturebackend.model.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserConverter extends Converter<User, UserVO> {

    UserLoginVO toLoginVO(User user);
}
