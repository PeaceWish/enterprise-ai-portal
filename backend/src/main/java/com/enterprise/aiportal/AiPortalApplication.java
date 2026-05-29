package com.enterprise.aiportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.mybatis.spring.annotation.MapperScan("com.enterprise.aiportal.mapper")
public class AiPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiPortalApplication.class, args);
    }
}
