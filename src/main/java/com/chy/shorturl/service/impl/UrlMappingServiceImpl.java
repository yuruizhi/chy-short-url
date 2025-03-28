package com.chy.shorturl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chy.shorturl.entity.UrlMapping;
import com.chy.shorturl.mapper.UrlMappingMapper;
import com.chy.shorturl.service.UrlMappingService;
import com.chy.shorturl.strategy.ShortUrlGenerateStrategy;
import com.chy.shorturl.strategy.ShortUrlGenerateStrategy.ShortCodeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * URL映射服务实现类
 *
 * @author Henry.Yu
 * @date 2023/07/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlMappingServiceImpl extends ServiceImpl<UrlMappingMapper, UrlMapping> implements UrlMappingService {

    private final ShortUrlGenerateStrategy shortUrlGenerateStrategy;
    private final StringRedisTemplate redisTemplate;

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
        
        // 缓存到Redis
        redisTemplate.opsForValue().set("shorturl:" + shortCode, originalUrl, cacheExpireSeconds, TimeUnit.SECONDS);
        
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
        // 先从Redis缓存获取
        String originalUrl = redisTemplate.opsForValue().get("shorturl:" + shortCode);
        
        if (originalUrl != null) {
            // 异步增加访问次数
            incrementAccessCountAsync(shortCode);
            return originalUrl;
        }
        
        // 缓存未命中，从数据库查询
        UrlMapping urlMapping = findByShortCode(shortCode);
        if (urlMapping == null) {
            return null;
        }
        
        // 检查链接是否过期
        if (urlMapping.getExpireTime() != null && urlMapping.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        
        // 更新访问次数
        baseMapper.incrementAccessCount(urlMapping.getId());
        
        // 更新缓存
        redisTemplate.opsForValue().set("shorturl:" + shortCode, urlMapping.getOriginalUrl(),
                cacheExpireSeconds, TimeUnit.SECONDS);
        
        return urlMapping.getOriginalUrl();
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
        // 这里可以使用线程池或消息队列异步处理
        new Thread(() -> {
            try {
                UrlMapping urlMapping = findByShortCode(shortCode);
                if (urlMapping != null) {
                    baseMapper.incrementAccessCount(urlMapping.getId());
                }
            } catch (Exception e) {
                log.error("增加访问次数失败: {}", e.getMessage(), e);
            }
        }).start();
    }
} 