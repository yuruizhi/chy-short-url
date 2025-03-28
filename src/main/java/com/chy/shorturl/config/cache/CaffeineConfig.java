package com.chy.shorturl.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine本地缓存配置
 *
 * @author Henry.Yu
 * @date 2024/04/27
 */
@Slf4j
@EnableCaching
@Configuration
public class CaffeineConfig {
    
    @Value("${shorturl.cache.local.shortUrl.size:10000}")
    private int shortUrlCacheSize;
    
    @Value("${shorturl.cache.local.shortUrl.expire-seconds:3600}")
    private int shortUrlExpireSeconds;
    
    @Value("${shorturl.cache.local.metadata.size:1000}")
    private int metadataCacheSize;
    
    @Value("${shorturl.cache.local.metadata.expire-seconds:1800}")
    private int metadataExpireSeconds;
    
    /**
     * 短链接本地缓存
     */
    @Bean
    public Cache<String, String> shortUrlLocalCache() {
        Cache<String, String> cache = Caffeine.newBuilder()
                .initialCapacity(1000)
                .maximumSize(shortUrlCacheSize)
                .expireAfterWrite(shortUrlExpireSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build();
        log.info("初始化短链接本地缓存, 容量: {}, 过期时间: {}秒", shortUrlCacheSize, shortUrlExpireSeconds);
        return cache;
    }
    
    /**
     * 短链接元数据本地缓存（用于短链接访问统计等非核心数据）
     */
    @Bean
    public Cache<String, Object> metadataLocalCache() {
        Cache<String, Object> cache = Caffeine.newBuilder()
                .initialCapacity(500)
                .maximumSize(metadataCacheSize)
                .expireAfterWrite(metadataExpireSeconds, TimeUnit.SECONDS)
                .recordStats()
                .build();
        log.info("初始化元数据本地缓存, 容量: {}, 过期时间: {}秒", metadataCacheSize, metadataExpireSeconds);
        return cache;
    }
    
    /**
     * Caffeine缓存管理器（用于@Cacheable等注解方式使用）
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(500)
                .maximumSize(5000)
                .expireAfterWrite(1800, TimeUnit.SECONDS)
                .recordStats());
        return cacheManager;
    }
} 