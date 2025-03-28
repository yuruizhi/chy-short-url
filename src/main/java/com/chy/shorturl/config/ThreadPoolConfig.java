package com.chy.shorturl.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author Henry.Yu
 * @date 2023/07/11
 */
@Slf4j
@EnableAsync
@Configuration
public class ThreadPoolConfig {

    @Value("${shorturl.thread.core-size:8}")
    private int corePoolSize;

    @Value("${shorturl.thread.max-size:16}")
    private int maxPoolSize;

    @Value("${shorturl.thread.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${shorturl.thread.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * 短链接统计线程池
     */
    @Bean("shortUrlTaskExecutor")
    public ThreadPoolTaskExecutor shortUrlTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        executor.setCorePoolSize(corePoolSize);
        // 设置最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        // 设置队列容量
        executor.setQueueCapacity(queueCapacity);
        // 设置线程活跃时间（秒）
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 设置默认线程名称
        executor.setThreadNamePrefix("short-url-");
        // 设置拒绝策略：由调用线程处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 初始化
        executor.initialize();
        log.info("初始化短链接线程池完成");
        return executor;
    }
} 