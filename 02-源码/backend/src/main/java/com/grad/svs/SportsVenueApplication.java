package com.grad.svs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 教师教研活动与成果管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.grad.svs.mapper")
public class SportsVenueApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsVenueApplication.class, args);
    }
}
