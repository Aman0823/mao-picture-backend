package com.example.maopicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.example.maopicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync

public class MaoPictureBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaoPictureBackendApplication.class, args);
    }

}
