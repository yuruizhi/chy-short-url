package com.chy.shorturl.config;

import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import com.chy.shorturl.strategy.impl.RandomShortUrlStrategy;
import com.chy.shorturl.strategy.impl.SnowflakeShortUrlStrategy;
import com.chy.shorturl.strategy.impl.Md5ShortUrlStrategy;
import com.chy.shorturl.strategy.impl.Murmur3ShortUrlStrategy;
import com.chy.shorturl.strategy.impl.CounterShortUrlStrategy;
import com.chy.shorturl.strategy.impl.Base64ShortUrlStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 短链接策略配置
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Configuration
@RequiredArgsConstructor
public class ShortUrlStrategyConfig {

    private final RandomShortUrlStrategy randomStrategy;
    private final SnowflakeShortUrlStrategy snowflakeStrategy;
    private final Md5ShortUrlStrategy md5Strategy;
    private final Murmur3ShortUrlStrategy murmur3Strategy;
    private final CounterShortUrlStrategy counterStrategy;
    private final Base64ShortUrlStrategy base64Strategy;
    
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
            case "MD5":
                return md5Strategy;
            case "MURMUR3":
                return murmur3Strategy;
            case "COUNTER":
                return counterStrategy;
            case "BASE64":
                return base64Strategy;
            case "RANDOM":
            default:
                return randomStrategy;
        }
    }
} 