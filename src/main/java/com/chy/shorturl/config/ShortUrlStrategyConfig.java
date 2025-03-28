package com.chy.shorturl.config;

import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import com.chy.shorturl.strategy.impl.RandomShortUrlStrategy;
import com.chy.shorturl.strategy.impl.SnowflakeShortUrlStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 短链接策略配置
 *
 * @author Henry.Yu
 * @date 2023/07/11
 */
@Configuration
@RequiredArgsConstructor
public class ShortUrlStrategyConfig {

    private final RandomShortUrlStrategy randomStrategy;
    private final SnowflakeShortUrlStrategy snowflakeStrategy;
    
    @Value("${shorturl.strategy:RANDOM}")
    private String strategyType;
    
    /**
     * 根据配置选择短链接生成策略
     *
     * @return 短链接生成策略
     */
    @Bean
    @Primary
    public ShortUrlGenerateStrategy shortUrlGenerateStrategy() {
        switch (strategyType.toUpperCase()) {
            case "SNOWFLAKE":
                return snowflakeStrategy;
            case "RANDOM":
            default:
                return randomStrategy;
        }
    }
} 