package com.chy.shorturl.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存监控接口
 *
 * @author Henry.Yu
 * @date 2024/04/27
 */
@RestController
@RequestMapping("/actuator/cache")
@RequiredArgsConstructor
public class CacheMetricsController {

    private final Cache<String, String> shortUrlLocalCache;
    private final Cache<String, Object> metadataLocalCache;

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, CacheMetrics>> getCacheStats() {
        Map<String, CacheMetrics> result = new HashMap<>(2);
        
        // 短链接缓存统计
        result.put("shortUrlCache", createCacheMetrics(shortUrlLocalCache));
        
        // 元数据缓存统计
        result.put("metadataCache", createCacheMetrics(metadataLocalCache));
        
        return Result.success(result);
    }
    
    /**
     * 创建缓存统计指标
     */
    private CacheMetrics createCacheMetrics(Cache<?, ?> cache) {
        CacheStats stats = cache.stats();
        CacheMetrics metrics = new CacheMetrics();
        
        metrics.setSize(cache.estimatedSize());
        metrics.setHitCount(stats.hitCount());
        metrics.setMissCount(stats.missCount());
        metrics.setHitRate(stats.hitRate());
        metrics.setMissRate(1 - stats.hitRate());
        metrics.setLoadSuccessCount(stats.loadSuccessCount());
        metrics.setLoadFailureCount(stats.loadFailureCount());
        metrics.setEvictionCount(stats.evictionCount());
        metrics.setTotalLoadTime(stats.totalLoadTime() / 1_000_000.0); // 纳秒转毫秒
        
        return metrics;
    }
    
    /**
     * 缓存统计指标
     */
    @Data
    public static class CacheMetrics {
        private long size;
        private long hitCount;
        private long missCount;
        private double hitRate;
        private double missRate;
        private long loadSuccessCount;
        private long loadFailureCount;
        private long evictionCount;
        private double totalLoadTime; // 毫秒
    }
} 