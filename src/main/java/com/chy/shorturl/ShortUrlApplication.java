package com.chy.shorturl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 短链接服务应用程序入口
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication
@MapperScan("com.chy.shorturl.mapper")
public class ShortUrlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortUrlApplication.class, args);
    }
} 