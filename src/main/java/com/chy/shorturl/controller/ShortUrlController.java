package com.chy.shorturl.controller;

import com.chy.shorturl.common.Result;
import com.chy.shorturl.common.aop.LogParam;
import com.chy.shorturl.common.aop.LogParam.LogLevel;
import com.chy.shorturl.common.util.LogUtil;
import com.chy.shorturl.service.UrlMappingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 短链接控制器
 *
 * @author Henry.Yu
 * @date 2024/04/27
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ShortUrlController {

    private final UrlMappingService urlMappingService;

    /**
     * 生成短链接
     *
     * @param request 请求参数
     * @return 短链接
     */
    @PostMapping("/api/url/shorten")
    @LogParam(
        desc = "生成短链接", 
        printResponse = true, 
        level = LogLevel.INFO,
        hideSensitive = true
    )
    public Result<String> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        try {
            String shortUrl = urlMappingService.generateShortUrl(request.getUrl(), request.getExpireTime());
            return Result.success(shortUrl);
        } catch (Exception e) {
            log.error("生成短链接失败: {}", e.getMessage());
            return Result.error("生成短链接失败");
        }
    }

    /**
     * 重定向到原始URL
     *
     * @param shortCode 短码
     * @return 重定向结果
     */
    @GetMapping("/{shortCode}")
    @LogParam(
        desc = "短链接重定向", 
        printResponse = false,
        level = LogLevel.DEBUG
    )
    public RedirectView redirect(@PathVariable String shortCode) {
        try {
            String originalUrl = urlMappingService.getOriginalUrl(shortCode);
            if (originalUrl != null) {
                return new RedirectView(originalUrl);
            } else {
                return new RedirectView("/error/404");
            }
        } catch (Exception e) {
            log.error("短链接重定向失败: {}", e.getMessage());
            return new RedirectView("/error/500");
        }
    }

    /**
     * 获取短链接统计信息
     *
     * @param shortCode 短码
     * @return 统计信息
     */
    @GetMapping("/api/url/stats/{shortCode}")
    @LogParam(
        desc = "获取短链接统计", 
        printResponse = true,
        responseMaxLength = 1000
    )
    public Result<Object> getStats(@PathVariable String shortCode) {
        // 实际项目中会返回真实的统计数据
        Map<String, Object> stats = new HashMap<>();
        stats.put("shortCode", shortCode);
        stats.put("visitCount", 100);
        stats.put("uniqueVisitors", 50);
        stats.put("lastAccessTime", LocalDateTime.now());
        
        return Result.success(stats);
    }

    /**
     * 请求参数
     */
    @Data
    public static class ShortenUrlRequest {
        /**
         * 原始URL
         */
        @NotBlank(message = "URL不能为空")
        private String url;

        /**
         * 过期时间（秒）
         */
        private Long expireTime;
        
        /**
         * 用户手机号（可选）
         */
        private String phone;
        
        /**
         * 用户邮箱（可选）
         */
        private String email;
    }
} 