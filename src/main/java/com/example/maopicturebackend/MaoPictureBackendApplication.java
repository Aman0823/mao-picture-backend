package com.example.maopicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.example.maopicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)

public class MaoPictureBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaoPictureBackendApplication.class, args);
    }

}
