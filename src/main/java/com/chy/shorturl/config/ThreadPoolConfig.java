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
 * @date 2024/04/27
 */
@Slf4j
@EnableAsync
@Configuration
public class ThreadPoolConfig {

    @Value("${shorturl.thread.core-size:10}")
    private int corePoolSize;

    @Value("${shorturl.thread.max-size:50}")
    private int maxPoolSize;

    @Value("${shorturl.thread.queue-capacity:2000}")
    private int queueCapacity;

    @Value("${shorturl.thread.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Value("${shorturl.thread.monitor.period-seconds:60}")
    private int monitorPeriodSeconds;

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
        // 设置线程池关闭的最大等待时间
        executor.setAwaitTerminationSeconds(60);
        // 设置线程池允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        // 初始化
        executor.initialize();
        log.info("短链接线程池初始化完成, 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                corePoolSize, maxPoolSize, queueCapacity);
                
        // 创建线程池监控任务
        startThreadPoolMonitor(executor);
        
        return executor;
    }
    
    /**
     * 启动线程池监控
     */
    private void startThreadPoolMonitor(ThreadPoolTaskExecutor executor) {
        Thread monitorThread = new Thread(() -> {
            ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 记录线程池状态
                    log.info("线程池状态: 活跃线程数: {}, 正在执行任务数: {}, 已完成任务数: {}, 任务总数: {}, 队列大小: {}",
                            threadPoolExecutor.getActiveCount(),
                            threadPoolExecutor.getTaskCount() - threadPoolExecutor.getCompletedTaskCount(),
                            threadPoolExecutor.getCompletedTaskCount(),
                            threadPoolExecutor.getTaskCount(),
                            threadPoolExecutor.getQueue().size());
                    
                    Thread.sleep(monitorPeriodSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setName("thread-pool-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
        log.info("启动线程池监控线程, 监控周期: {}秒", monitorPeriodSeconds);
    }
} 