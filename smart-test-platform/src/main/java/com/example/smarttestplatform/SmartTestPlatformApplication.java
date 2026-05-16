package com.example.smarttestplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.smarttestplatform.module.*.mapper")

public class SmartTestPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartTestPlatformApplication.class, args);
    }
}