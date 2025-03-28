package com.chy.shorturl.service;

import com.chy.shorturl.entity.UrlMapping;

/**
 * URL映射服务接口
 *
 * @author Henry.Yu
 * @date 2023/07/11
 */
public interface UrlMappingService {

    /**
     * 生成短链接
     *
     * @param originalUrl 原始URL
     * @param expireTime  过期时间（可选）
     * @return 短链接
     */
    String generateShortUrl(String originalUrl, Long expireTime);

    /**
     * 获取原始URL
     *
     * @param shortCode 短码
     * @return 原始URL
     */
    String getOriginalUrl(String shortCode);

    /**
     * 根据短码查询URL映射
     *
     * @param shortCode 短码
     * @return URL映射对象
     */
    UrlMapping findByShortCode(String shortCode);
} 