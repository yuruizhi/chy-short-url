package com.chy.shorturl.strategy.impl;

import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于MurmurHash3的短链接生成策略
 * Murmur3哈希算法速度快，哈希分布均匀，适合短链接生成
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Component
@RequiredArgsConstructor
public class Murmur3ShortUrlStrategy implements ShortUrlGenerateStrategy {
    
    @Value("${shorturl.length:6}")
    private int urlLength;
    
    /**
     * 字符集，用于生成短链接
     */
    private static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    
    /**
     * 基于原始URL和随机数生成短链接
     *
     * @param url 原始URL
     * @param validator 短码验证器，用于检查短码是否已存在
     * @return 生成的短链接
     */
    @Override
    public String generateShortUrl(String url, ShortCodeValidator validator) {
        String shortCode;
        do {
            shortCode = generateMurmurHashShortCode(url);
        } while (validator.exists(shortCode));
        
        return shortCode;
    }
    
    /**
     * 生成基于MurmurHash3的短码
     *
     * @param url 原始URL
     * @return 短码
     */
    private String generateMurmurHashShortCode(String url) {
        // 为防止同一URL多次生成不同短链接，添加随机数
        String randomSalt = String.valueOf(ThreadLocalRandom.current().nextInt(10000));
        String input = url + randomSalt + System.currentTimeMillis();
        
        // 计算MurmurHash3哈希
        int hash = murmur3Hash32(input.getBytes(StandardCharsets.UTF_8));
        
        // 确保hash值为正数
        long positiveHash = hash & 0xFFFFFFFFL;
        
        // 转换为短码字符串
        return toBase62(positiveHash, urlLength);
    }
    
    /**
     * 将数值转为指定长度的62进制字符串
     *
     * @param number 数值
     * @param length 期望长度
     * @return 62进制字符串
     */
    private String toBase62(long number, int length) {
        StringBuilder sb = new StringBuilder();
        int base = CHARS.length;
        
        // 转换为62进制
        do {
            int index = (int) (number % base);
            sb.append(CHARS[index]);
            number /= base;
        } while (number > 0);
        
        // 长度不足则补齐
        while (sb.length() < length) {
            sb.append(CHARS[ThreadLocalRandom.current().nextInt(CHARS.length)]);
        }
        
        // 长度超过则截断
        if (sb.length() > length) {
            return sb.substring(0, length);
        }
        
        return sb.toString();
    }
    
    /**
     * MurmurHash3 32位实现
     * 
     * @param data 输入数据
     * @return 哈希值
     */
    private int murmur3Hash32(byte[] data) {
        final int seed = ThreadLocalRandom.current().nextInt();
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;
        final int r1 = 15;
        final int r2 = 13;
        final int m = 5;
        final int n = 0xe6546b64;
        
        int hash = seed;
        int len = data.length;
        int i = 0;
        
        // 主循环，每次处理4个字节
        while (len >= 4) {
            int k = ((data[i] & 0xff)) |
                    ((data[i + 1] & 0xff) << 8) |
                    ((data[i + 2] & 0xff) << 16) |
                    ((data[i + 3] & 0xff) << 24);
            
            k *= c1;
            k = Integer.rotateLeft(k, r1);
            k *= c2;
            
            hash ^= k;
            hash = Integer.rotateLeft(hash, r2);
            hash = hash * m + n;
            
            i += 4;
            len -= 4;
        }
        
        // 处理剩余的字节
        int k = 0;
        switch (len) {
            case 3:
                k ^= (data[i + 2] & 0xff) << 16;
            case 2:
                k ^= (data[i + 1] & 0xff) << 8;
            case 1:
                k ^= (data[i] & 0xff);
                k *= c1;
                k = Integer.rotateLeft(k, r1);
                k *= c2;
                hash ^= k;
        }
        
        // 最终混合
        hash ^= data.length;
        hash ^= hash >>> 16;
        hash *= 0x85ebca6b;
        hash ^= hash >>> 13;
        hash *= 0xc2b2ae35;
        hash ^= hash >>> 16;
        
        return hash;
    }
} 