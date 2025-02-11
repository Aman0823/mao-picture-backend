package com.example.maopicturebackend.controller;

import com.example.maopicturebackend.common.BaseResponse;
import com.example.maopicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.success("ok");
    }
}
