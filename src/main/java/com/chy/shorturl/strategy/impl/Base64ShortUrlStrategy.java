package com.chy.shorturl.strategy.impl;

import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于Base64编码的短链接生成策略
 * 适合需要可逆转换的场景，可以将有规则的信息编码为短链接
 *
 * @author Henry.Yu
 * @date 2024/04/27
 */
@Component
@RequiredArgsConstructor
public class Base64ShortUrlStrategy implements ShortUrlGenerateStrategy {
    
    @Value("${shorturl.length:6}")
    private int urlLength;
    
    /**
     * 自定义Base64字符集，URL安全且排序不同以区分生成的短链接
     */
    private static final char[] URL_SAFE_CHARS = 
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();
    
    /**
     * 基于原始URL生成短链接
     *
     * @param url 原始URL
     * @param validator 短码验证器，用于检查短码是否已存在
     * @return 生成的短链接
     */
    @Override
    public String generateShortUrl(String url, ShortCodeValidator validator) {
        String shortCode;
        do {
            shortCode = generateBase64ShortCode(url);
        } while (validator.exists(shortCode));
        
        return shortCode;
    }
    
    /**
     * 生成Base64短码
     *
     * @param url 原始URL
     * @return 短码
     */
    private String generateBase64ShortCode(String url) {
        // 生成一个随机UUID作为原始数据
        String randomUUID = UUID.randomUUID().toString();
        
        // 将URL和随机UUID组合，添加时间戳增加唯一性
        String input = url + "|" + randomUUID + "|" + System.currentTimeMillis();
        
        // Base64编码
        String base64 = Base64.getUrlEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
        
        // 替换标准Base64中的'='填充字符
        base64 = base64.replace("=", "");
        
        // 如果编码结果长度不足，则直接使用随机字符补全
        if (base64.length() < urlLength) {
            StringBuilder sb = new StringBuilder(base64);
            int deficit = urlLength - base64.length();
            for (int i = 0; i < deficit; i++) {
                int randomIndex = ThreadLocalRandom.current().nextInt(URL_SAFE_CHARS.length);
                sb.append(URL_SAFE_CHARS[randomIndex]);
            }
            base64 = sb.toString();
        }
        
        // 从Base64编码结果中随机选取一段作为短码，增加变化性
        int startPos = 0;
        if (base64.length() > urlLength) {
            startPos = ThreadLocalRandom.current().nextInt(base64.length() - urlLength);
        }
        
        return base64.substring(startPos, startPos + urlLength);
    }
    
    /**
     * 混淆Base64字符串，增加安全性并增加短码多样性
     *
     * @param input 原始Base64字符串
     * @return 混淆后的字符串
     */
    private String obfuscateBase64(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        char[] chars = input.toCharArray();
        int seed = ThreadLocalRandom.current().nextInt(1000);
        
        // 简单的混淆算法，可以根据需要调整
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            int index = -1;
            
            // 查找字符在URL_SAFE_CHARS中的位置
            for (int j = 0; j < URL_SAFE_CHARS.length; j++) {
                if (URL_SAFE_CHARS[j] == c) {
                    index = j;
                    break;
                }
            }
            
            if (index >= 0) {
                // 根据seed和位置进行位移，实现混淆
                int newIndex = (index + seed + i) % URL_SAFE_CHARS.length;
                chars[i] = URL_SAFE_CHARS[newIndex];
            }
        }
        
        return new String(chars);
    }
} 