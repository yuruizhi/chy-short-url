package com.chy.shorturl.controller;

import com.chy.shorturl.common.Result;
import com.chy.shorturl.service.UrlMappingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import com.chy.shorturl.common.util.LogUtil;

/**
 * 短链接控制器
 *
 * @author Henry.Yu
 * @date 2025/03/28
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
    public Result<String> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        String requestId = LogUtil.getRequestId();
        log.info("生成短链接请求开始处理, requestId: {}, 请求参数: {}", requestId, request);
        try {
            String shortUrl = urlMappingService.generateShortUrl(request.getUrl(), request.getExpireTime());
            log.info("短链接生成成功, requestId: {}, 短链接: {}", requestId, shortUrl);
            return Result.success(shortUrl);
        } catch (Exception e) {
            log.error("生成短链接失败, requestId: {}, 错误信息: {}", requestId, e.getMessage(), e);
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
    public RedirectView redirect(@PathVariable String shortCode) {
        String requestId = LogUtil.getRequestId();
        log.info("短链接访问开始处理, requestId: {}, 短码: {}", requestId, shortCode);
        try {
            String originalUrl = urlMappingService.getOriginalUrl(shortCode);
            if (originalUrl != null) {
                log.info("短链接重定向成功, requestId: {}, 短码: {}, 原始URL: {}", requestId, shortCode, originalUrl);
                return new RedirectView(originalUrl);
            } else {
                log.warn("短链接不存在或已过期, requestId: {}, 短码: {}", requestId, shortCode);
                return new RedirectView("/error/404");
            }
        } catch (Exception e) {
            log.error("短链接重定向失败, requestId: {}, 短码: {}, 错误信息: {}", requestId, shortCode, e.getMessage(), e);
            return new RedirectView("/error/500");
        }
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
    }
} 