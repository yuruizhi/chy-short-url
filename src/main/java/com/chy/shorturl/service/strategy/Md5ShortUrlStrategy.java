package com.chy.shorturl.service.strategy;

import com.chy.shorturl.service.ShortUrlStrategy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于MD5哈希算法的短链接生成策略
 *
 * @author Henry.Yu
 * @date 2024/04/27
 */
@Component
public class Md5ShortUrlStrategy implements ShortUrlStrategy {

    /**
     * 字符集，用于生成短链接
     */
    private static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    
    /**
     * 基于原始URL和随机数生成短链接
     *
     * @param url 原始URL
     * @param length 短链接长度
     * @return 生成的短链接
     */
    @Override
    public String generateShortUrl(String url, int length) {
        try {
            // 为防止同一URL多次生成不同短链接，添加随机数
            String randomSalt = String.valueOf(ThreadLocalRandom.current().nextInt(10000));
            String input = url + randomSalt + System.currentTimeMillis();
            
            // 计算MD5哈希
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 将bytes转为16进制字符串
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            
            // 从MD5的32位16进制字符串中选择一个子串作为短链接的基础
            // 可以选择不同的位置来增加变化性
            int startIndex = ThreadLocalRandom.current().nextInt(32 - length);
            String md5Sub = hex.substring(startIndex, startIndex + length);
            
            // 将子串映射到CHARS字符集中，增加可读性
            char[] shortUrlChars = new char[length];
            for (int i = 0; i < length; i++) {
                // 将16进制字符转换为0-15之间的数值
                int value = Character.digit(md5Sub.charAt(i), 16);
                // 将0-15的数值映射到CHARS的索引
                int charIndex = (value * 4) % CHARS.length; // 乘以4扩大分布范围
                shortUrlChars[i] = CHARS[charIndex];
            }
            
            return new String(shortUrlChars);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }
    
    @Override
    public String getName() {
        return "md5";
    }
} 