package com.chy.shorturl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 性能测试类
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PerformanceTest {

    /**
     * 短链接生成性能测试
     * 
     * 测试并发生成短链接的性能
     * 
     * @throws InterruptedException 中断异常
     */
    @Test
    public void testShortenUrlPerformance() throws InterruptedException {
        // 测试参数
        int threadCount = 100; // 并发线程数
        int requestPerThread = 10; // 每个线程请求次数
        int totalRequests = threadCount * requestPerThread;
        
        // 统计数据
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        TestRestTemplate restTemplate = new TestRestTemplate();
        
        // 请求URL
        String url = "http://localhost:8080/api/url/shorten";
        
        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        long startTime = System.currentTimeMillis();
        
        // 启动多线程测试
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executorService.execute(() -> {
                try {
                    for (int j = 0; j < requestPerThread; j++) {
                        // 构建请求体
                        Map<String, Object> requestBody = new HashMap<>();
                        requestBody.put("url", "https://www.example.com/test/performance/" + threadIndex + "/" + j);
                        requestBody.put("expireTime", 86400L);
                        
                        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                        
                        try {
                            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                            if (response.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            log.error("请求失败: {}", e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        latch.await();
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 输出测试结果
        log.info("====== 短链接生成性能测试结果 ======");
        log.info("并发线程数: {}", threadCount);
        log.info("总请求数: {}", totalRequests);
        log.info("成功请求数: {}", successCount.get());
        log.info("失败请求数: {}", errorCount.get());
        log.info("总耗时(ms): {}", totalTime);
        log.info("TPS: {}", 1000.0 * successCount.get() / totalTime);
        log.info("平均响应时间(ms): {}", (double) totalTime / successCount.get());
        log.info("===================================");
    }
    
    /**
     * 短链接访问性能测试
     * 
     * 测试并发访问短链接的性能
     * 
     * @throws InterruptedException 中断异常
     */
    @Test
    public void testAccessShortUrlPerformance() throws InterruptedException {
        // 测试参数
        int threadCount = 200; // 并发线程数
        int requestPerThread = 50; // 每个线程请求次数
        int totalRequests = threadCount * requestPerThread;
        
        // 预先生成测试用的短链接
        String shortCode = "abcde1"; // 假设这是预先生成好的短码
        
        // 统计数据
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        TestRestTemplate restTemplate = new TestRestTemplate();
        
        // 请求URL
        String url = "http://localhost:8080/" + shortCode;
        
        long startTime = System.currentTimeMillis();
        
        // 启动多线程测试
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    for (int j = 0; j < requestPerThread; j++) {
                        try {
                            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection()) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            log.error("请求失败: {}", e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        latch.await();
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 输出测试结果
        log.info("====== 短链接访问性能测试结果 ======");
        log.info("并发线程数: {}", threadCount);
        log.info("总请求数: {}", totalRequests);
        log.info("成功请求数: {}", successCount.get());
        log.info("失败请求数: {}", errorCount.get());
        log.info("总耗时(ms): {}", totalTime);
        log.info("QPS: {}", 1000.0 * successCount.get() / totalTime);
        log.info("平均响应时间(ms): {}", (double) totalTime / successCount.get());
        log.info("===================================");
    }
} 