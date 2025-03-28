package com.chy.shorturl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chy.shorturl.entity.UrlMapping;
import com.chy.shorturl.mapper.UrlMappingMapper;
import com.chy.shorturl.service.UrlMappingService;
import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import com.chy.shorturl.strategy.ShortUrlGenerateStrategy.ShortCodeValidator;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.chy.shorturl.common.util.LogUtil;

/**
 * URL映射服务实现类
 *
 * @author Henry.Yu
 * @date 2024/04/27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlMappingServiceImpl extends ServiceImpl<UrlMappingMapper, UrlMapping> implements UrlMappingService {

    private final ShortUrlGenerateStrategy shortUrlGenerateStrategy;
    private final StringRedisTemplate redisTemplate;
    private final Cache<String, String> shortUrlLocalCache;
    private final Cache<String, Object> metadataLocalCache;
    
    @Qualifier("shortUrlTaskExecutor")
    private final ThreadPoolTaskExecutor taskExecutor;

    @Value("${shorturl.domain}")
    private String domain;

    @Value("${shorturl.cache-expire:86400}")
    private long cacheExpireSeconds;

    /**
     * 生成短链接
     *
     * @param originalUrl 原始URL
     * @param expireTime  过期时间（秒）
     * @return 短链接
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateShortUrl(String originalUrl, Long expireTime) {
        log.info("生成短链接，原始URL: {}, 过期时间: {}, requestId: {}", originalUrl, expireTime, LogUtil.getRequestId());
        
        // 生成短码
        String shortCode = shortUrlGenerateStrategy.generateShortUrl(originalUrl, code -> findByShortCode(code) != null);
        
        // 构建短链接
        String shortUrl = domain + "/" + shortCode;
        
        // 设置过期时间
        LocalDateTime expireDateTime = null;
        if (expireTime != null && expireTime > 0) {
            expireDateTime = LocalDateTime.now().plusSeconds(expireTime);
        }
        
        // 保存映射关系到数据库
        UrlMapping urlMapping = new UrlMapping()
                .setOriginalUrl(originalUrl)
                .setShortCode(shortCode)
                .setExpireTime(expireDateTime)
                .setAccessCount(0L)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now())
                .setIsDeleted(0);
        
        save(urlMapping);
        
        // 同步缓存到Redis和本地缓存
        String cacheKey = "shorturl:" + shortCode;
        redisTemplate.opsForValue().set(cacheKey, originalUrl, cacheExpireSeconds, TimeUnit.SECONDS);
        shortUrlLocalCache.put(shortCode, originalUrl);
        
        return shortUrl;
    }
    
    /**
     * 获取原始URL
     *
     * @param shortCode 短码
     * @return 原始URL
     */
    @Override
    public String getOriginalUrl(String shortCode) {
        // 先从本地缓存获取
        String originalUrl = shortUrlLocalCache.getIfPresent(shortCode);
        if (originalUrl != null) {
            // 异步增加访问次数
            incrementAccessCountAsync(shortCode);
            return originalUrl;
        }
        
        // 本地缓存未命中，从Redis获取
        String cacheKey = "shorturl:" + shortCode;
        originalUrl = redisTemplate.opsForValue().get(cacheKey);
        
        if (originalUrl != null) {
            // 放入本地缓存
            shortUrlLocalCache.put(shortCode, originalUrl);
            // 异步增加访问次数
            incrementAccessCountAsync(shortCode);
            return originalUrl;
        }
        
        // Redis缓存未命中，从数据库查询
        UrlMapping urlMapping = findByShortCode(shortCode);
        if (urlMapping == null) {
            return null;
        }
        
        // 检查链接是否过期
        if (urlMapping.getExpireTime() != null && urlMapping.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        
        // 更新访问次数
        incrementAccessCountAsync(shortCode);
        
        originalUrl = urlMapping.getOriginalUrl();
        
        // 更新缓存
        redisTemplate.opsForValue().set(cacheKey, originalUrl, cacheExpireSeconds, TimeUnit.SECONDS);
        shortUrlLocalCache.put(shortCode, originalUrl);
        
        return originalUrl;
    }
    
    /**
     * 根据短码查询URL映射
     *
     * @param shortCode 短码
     * @return URL映射对象
     */
    @Override
    public UrlMapping findByShortCode(String shortCode) {
        return baseMapper.findByShortCode(shortCode);
    }
    
    /**
     * 异步增加访问次数
     *
     * @param shortCode 短码
     */
    private void incrementAccessCountAsync(String shortCode) {
        // 使用元数据缓存记录待更新的访问次数
        String countKey = "count:" + shortCode;
        Long count = (Long) metadataLocalCache.get(countKey, k -> 0L);
        metadataLocalCache.put(countKey, count + 1);
        
        // 使用线程池异步处理数据库更新
        CompletableFuture.runAsync(() -> {
            try {
                UrlMapping urlMapping = findByShortCode(shortCode);
                if (urlMapping != null) {
                    baseMapper.incrementAccessCount(urlMapping.getId());
                }
            } catch (Exception e) {
                log.error("异步增加访问次数失败: {}", e.getMessage(), e);
            }
        }, taskExecutor);
    }
    
    /**
     * 同步访问统计数据到数据库
     * 这个方法可以由定时任务调用，批量更新访问计数
     */
    @Async("shortUrlTaskExecutor")
    public void syncAccessCountToDb() {
        // 实际项目中，这里可以实现批量更新逻辑
        log.info("同步访问统计数据到数据库");
    }
} 