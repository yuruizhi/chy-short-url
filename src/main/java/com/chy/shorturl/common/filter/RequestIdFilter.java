package com.chy.shorturl.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求ID过滤器，用于生成每个请求的唯一标识
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String MDC_REQUEST_ID_KEY = "requestId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        
        // 如果请求头中没有请求ID，则生成一个
        if (requestId == null || requestId.isEmpty()) {
            requestId = generateRequestId();
        }
        
        // 将请求ID添加到MDC上下文
        MDC.put(MDC_REQUEST_ID_KEY, requestId);
        
        // 将请求ID添加到响应头
        response.setHeader(REQUEST_ID_HEADER, requestId);
        
        try {
            log.debug("开始处理请求，URI: {}, Method: {}", request.getRequestURI(), request.getMethod());
            // 执行过滤器链
            filterChain.doFilter(request, response);
            log.debug("请求处理完成，状态码: {}", response.getStatus());
        } finally {
            // 清理MDC上下文，避免内存泄漏
            MDC.remove(MDC_REQUEST_ID_KEY);
        }
    }
    
    /**
     * 生成请求ID
     *
     * @return 请求ID字符串
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
} 