package com.chy.shorturl.strategy.impl;

import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 随机策略生成短链接
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Component
@RequiredArgsConstructor
public class RandomShortUrlStrategy implements ShortUrlGenerateStrategy {
    
    @Value("${shorturl.length:6}")
    private int urlLength;
    
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new Random();
    
    @Override
    public String generateShortUrl(String url, ShortCodeValidator validator) {
        String shortCode;
        do {
            // 生成指定长度的随机短码
            shortCode = generateRandomCode(urlLength);
            // 检查短码是否已存在，如果存在则重新生成
        } while (validator.exists(shortCode));
        
        return shortCode;
    }
    
    /**
     * 生成随机短码
     *
     * @param length 长度
     * @return 随机短码
     */
    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
} 