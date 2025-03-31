package com.chy.shorturl.strategy.impl;

import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于自增计数器的短链接生成策略
 * 使用Redis全局计数器保证分布式环境下ID唯一
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Component
@RequiredArgsConstructor
public class CounterShortUrlStrategy implements ShortUrlGenerateStrategy {
    
    private final StringRedisTemplate redisTemplate;
    
    @Value("${shorturl.length:6}")
    private int urlLength;
    
    /**
     * Redis计数器键名
     */
    private static final String COUNTER_KEY = "shorturl:counter";
    
    /**
     * 本地缓冲计数器，减少Redis访问
     */
    private final AtomicLong localCounter = new AtomicLong(0);
    
    /**
     * 批次大小，每次从Redis获取的ID数量
     */
    private static final int BATCH_SIZE = 1000;
    
    /**
     * 当前批次的起始ID
     */
    private long currentBatchStart = 0;
    
    /**
     * 字符集，用于生成短链接，62个字符
     */
    private static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    
    /**
     * 基于自增计数器生成短链接
     *
     * @param url 原始URL
     * @param validator 短码验证器，用于检查短码是否已存在
     * @return 生成的短链接
     */
    @Override
    public String generateShortUrl(String url, ShortCodeValidator validator) {
        // 获取递增ID
        long id = getNextId();
        
        // 转换为短码
        String shortCode = toBase62(id);
        
        // 如果短码长度不足，补充前导零
        if (shortCode.length() < urlLength) {
            shortCode = padWithLeadingZeros(shortCode, urlLength);
        }
        
        // 如果短码长度超过限制，截取最后urlLength位
        if (shortCode.length() > urlLength) {
            shortCode = shortCode.substring(shortCode.length() - urlLength);
        }
        
        // 验证短码是否存在，存在则递归生成新的
        if (validator.exists(shortCode)) {
            return generateShortUrl(url, validator);
        }
        
        return shortCode;
    }
    
    /**
     * 获取下一个ID
     *
     * @return 唯一ID
     */
    private synchronized long getNextId() {
        // 计算当前ID在批次中的位置
        long localOffset = localCounter.incrementAndGet();
        
        // 如果当前批次已耗尽或首次运行，从Redis获取新批次
        if (localOffset > BATCH_SIZE || currentBatchStart == 0) {
            // 从Redis原子获取下一个批次起始值
            currentBatchStart = redisTemplate.opsForValue().increment(COUNTER_KEY, BATCH_SIZE);
            // 修正起始值，使其为批次的第一个ID
            currentBatchStart = currentBatchStart - BATCH_SIZE + 1;
            // 重置本地计数器
            localCounter.set(1);
            localOffset = 1;
        }
        
        // 当前ID = 批次起始值 + 本地偏移量 - 1
        return currentBatchStart + localOffset - 1;
    }
    
    /**
     * 将数字转换为62进制表示
     *
     * @param number 数字
     * @return 62进制字符串
     */
    private String toBase62(long number) {
        StringBuilder sb = new StringBuilder();
        int base = CHARS.length;
        
        do {
            int index = (int) (number % base);
            sb.append(CHARS[index]);
            number /= base;
        } while (number > 0);
        
        // 需要反转字符串，因为我们是从低位到高位生成的
        return sb.reverse().toString();
    }
    
    /**
     * 在字符串前面补充前导零
     *
     * @param str 原始字符串
     * @param length 目标长度
     * @return 补充后的字符串
     */
    private String padWithLeadingZeros(String str, int length) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < length) {
            sb.insert(0, CHARS[0]); // 使用字符集的第一个字符作为'0'
        }
        return sb.toString();
    }
}