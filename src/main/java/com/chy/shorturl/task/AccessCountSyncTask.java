package com.chy.shorturl.task;

import com.chy.shorturl.service.impl.UrlMappingServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 访问计数同步定时任务
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class AccessCountSyncTask {

    private final UrlMappingServiceImpl urlMappingService;

    /**
     * 定时同步访问计数到数据库
     * 每分钟执行一次
     */
    @Scheduled(fixedRateString = "${shorturl.task.sync-count-interval:60000}")
    public void syncAccessCount() {
        log.debug("开始执行访问计数同步任务");
        long startTime = System.currentTimeMillis();
        
        try {
            urlMappingService.syncAccessCountToDb();
        } catch (Exception e) {
            log.error("访问计数同步任务异常: {}", e.getMessage(), e);
        }
        
        long costTime = System.currentTimeMillis() - startTime;
        log.debug("访问计数同步任务完成, 耗时: {}ms", costTime);
    }
} 