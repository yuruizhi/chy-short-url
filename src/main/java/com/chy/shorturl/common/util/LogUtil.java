package com.chy.shorturl.common.util;

import org.slf4j.MDC;

/**
 * 日志工具类
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
public class LogUtil {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String DEFAULT_REQUEST_ID = "unknown";

    private LogUtil() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前请求ID
     *
     * @return 请求ID
     */
    public static String getRequestId() {
        String requestId = MDC.get(REQUEST_ID_KEY);
        return requestId != null ? requestId : DEFAULT_REQUEST_ID;
    }

    /**
     * 设置请求ID
     *
     * @param requestId 请求ID
     */
    public static void setRequestId(String requestId) {
        if (requestId != null && !requestId.isEmpty()) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }
    }

    /**
     * 清除请求ID
     */
    public static void clearRequestId() {
        MDC.remove(REQUEST_ID_KEY);
    }
} 