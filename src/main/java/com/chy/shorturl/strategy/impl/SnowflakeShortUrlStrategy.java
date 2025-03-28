package com.chy.shorturl.strategy.impl;

import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法生成短链接
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Component
@RequiredArgsConstructor
public class SnowflakeShortUrlStrategy implements ShortUrlGenerateStrategy {
    
    @Value("${shorturl.length:6}")
    private int urlLength;
    
    // 开始时间戳，用于减小时间戳的值
    private static final long START_TIMESTAMP = 1609459200000L; // 2021-01-01 00:00:00
    
    // 机器ID所占的位数
    private static final long WORKER_ID_BITS = 5L;
    
    // 数据中心ID所占的位数
    private static final long DATA_CENTER_ID_BITS = 5L;
    
    // 序列号所占的位数
    private static final long SEQUENCE_BITS = 12L;
    
    // 工作机器ID的最大值
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    
    // 数据中心ID的最大值
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    
    // 序列号的最大值
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    
    // 机器ID向左移12位
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    
    // 数据中心ID向左移17位(12+5)
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    
    // 时间戳向左移22位(12+5+5)
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
    
    // 基于62进制的字符集
    private static final char[] DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;
    
    // 序列号
    private long sequence = 0L;
    
    // 工作机器ID(0~31)
    private final long workerId = 1L;
    
    // 数据中心ID(0~31)
    private final long dataCenterId = 1L;
    
    @Override
    public String generateShortUrl(String url, ShortCodeValidator validator) {
        // 生成雪花ID
        long id = nextId();
        // 将雪花ID转为62进制编码
        String shortCode = toBase62(id);
        // 如果短码长度不满足要求，则补全或截断
        if (shortCode.length() < urlLength) {
            shortCode = padRight(shortCode, urlLength);
        } else if (shortCode.length() > urlLength) {
            shortCode = shortCode.substring(0, urlLength);
        }
        
        // 如果短码已存在，则重新生成
        if (validator.exists(shortCode)) {
            return generateShortUrl(url, validator);
        }
        
        return shortCode;
    }
    
    /**
     * 生成下一个ID
     *
     * @return 雪花ID
     */
    private synchronized long nextId() {
        long timestamp = timeGen();
        
        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过，抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }
        
        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 毫秒内序列溢出，阻塞到下一个毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }
        
        // 上次生成ID的时间戳
        lastTimestamp = timestamp;
        
        // 移位并通过或运算拼接组成64位的ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
    
    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 新的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    
    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
    
    /**
     * 将长整型数值转换为62进制字符串
     *
     * @param number 数值
     * @return 62进制字符串
     */
    private String toBase62(long number) {
        StringBuilder sb = new StringBuilder();
        int base = DIGITS.length;
        
        do {
            int index = (int) (number % base);
            sb.append(DIGITS[index]);
            number /= base;
        } while (number > 0);
        
        return sb.reverse().toString();
    }
    
    /**
     * 右侧补全字符串
     *
     * @param str    原字符串
     * @param length 目标长度
     * @return 补全后的字符串
     */
    private String padRight(String str, int length) {
        int padLength = length - str.length();
        StringBuilder sb = new StringBuilder(str);
        
        for (int i = 0; i < padLength; i++) {
            sb.append(DIGITS[(int) (Math.random() * DIGITS.length)]);
        }
        
        return sb.toString();
    }
} 