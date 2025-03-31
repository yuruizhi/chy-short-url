package com.chy.shorturl.common.aop;

import com.chy.shorturl.common.util.LogUtil;
import com.chy.shorturl.common.aop.LogParam.LogLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import com.chy.shorturl.common.util.SensitiveUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志切面，用于实现LogParam注解的功能
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final ObjectMapper objectMapper;
    
    /**
     * 敏感字段集合，用于字段脱敏
     */
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "password", "pwd", "passwd", "secret", "token", "credential",
            "creditCard", "credit_card", "cardNo", "card_no",
            "phone", "mobile", "idCard", "id_card", "ssn", "email"));
    
    /**
     * 手机号正则表达式
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}");
    
    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
    
    /**
     * 定义切点：所有添加了LogParam注解的方法
     */
    @Pointcut("@annotation(com.chy.shorturl.common.aop.LogParam)")
    public void logPointcut() {
    }
    
    /**
     * 环绕通知：处理请求参数和响应结果的日志记录
     */
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前请求ID
        String requestId = LogUtil.getRequestId();
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取LogParam注解
        LogParam logParam = method.getAnnotation(LogParam.class);
        // 获取方法名
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        // 获取日志描述
        String desc = logParam.desc().isEmpty() ? methodName : logParam.desc();
        
        long startTime = System.currentTimeMillis();
        
        // 打印请求参数
        if (logParam.printRequest()) {
            printRequestLog(joinPoint, methodName, desc, requestId, logParam);
        }
        
        // 打印请求头
        if (logParam.printHeaders()) {
            printHeadersLog(requestId, logParam.level());
        }
        
        // 执行原方法
        Object result = null;
        try {
            result = joinPoint.proceed();
            
            // 记录执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 打印响应结果
            if (logParam.printResponse()) {
                printResponseLog(result, methodName, executionTime, requestId, logParam);
            } else {
                logByLevel(logParam.level(), "[{}] 执行完成, 耗时: {}ms, requestId: {}", 
                        desc, executionTime, requestId);
            }
            
            return result;
        } catch (Throwable e) {
            // 打印异常信息
            if (logParam.printException()) {
                log.error("[{}] 执行异常, requestId: {}, 异常信息: {}", desc, requestId, e.getMessage(), e);
            } else {
                log.error("[{}] 执行异常, requestId: {}, 异常信息: {}", desc, requestId, e.getMessage());
            }
            throw e;
        }
    }
    
    /**
     * 根据日志级别打印日志
     */
    private void logByLevel(LogLevel level, String format, Object... arguments) {
        switch (level) {
            case DEBUG:
                log.debug(format, arguments);
                break;
            case WARN:
                log.warn(format, arguments);
                break;
            case INFO:
            default:
                log.info(format, arguments);
                break;
        }
    }
    
    /**
     * 打印请求参数日志
     */
    private void printRequestLog(JoinPoint joinPoint, String methodName, String desc, 
                                String requestId, LogParam logParam) {
        try {
            // 获取参数名
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            // 构建参数Map
            Map<String, Object> paramsMap = new HashMap<>(parameterNames.length);
            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                Object arg = args[i];
                
                // 文件类型特殊处理
                if (arg instanceof MultipartFile) {
                    MultipartFile file = (MultipartFile) arg;
                    paramsMap.put(paramName, String.format("fileName=%s, fileSize=%d", 
                            file.getOriginalFilename(), file.getSize()));
                } else {
                    // 敏感信息处理
                    if (logParam.hideSensitive() && isSensitiveField(paramName)) {
                        paramsMap.put(paramName, "******");
                    } else {
                        paramsMap.put(paramName, arg);
                    }
                }
            }
            
            String params = objectMapper.writeValueAsString(paramsMap);
            
            // 如果启用了敏感信息隐藏，则对JSON字符串中的敏感信息进行脱敏
            if (logParam.hideSensitive()) {
                params = maskSensitiveInfo(params);
            }
            
            logByLevel(logParam.level(), "[{}] 开始执行, 请求参数: {}, requestId: {}", 
                    desc, params, requestId);
        } catch (Exception e) {
            log.warn("解析请求参数异常, methodName: {}, requestId: {}, 异常信息: {}", 
                    methodName, requestId, e.getMessage());
        }
    }
    
    /**
     * 打印响应结果日志
     */
    private void printResponseLog(Object result, String methodName, long executionTime, 
                                 String requestId, LogParam logParam) {
        try {
            String responseStr = result != null ? objectMapper.writeValueAsString(result) : "null";
            
            // 响应结果长度限制
            if (logParam.responseMaxLength() > 0 && responseStr.length() > logParam.responseMaxLength()) {
                responseStr = responseStr.substring(0, logParam.responseMaxLength()) + "...(已截断)";
            }
            
            // 敏感信息处理
            if (logParam.hideSensitive()) {
                responseStr = maskSensitiveInfo(responseStr);
            }
            
            logByLevel(logParam.level(), "[{}] 执行完成, 响应结果: {}, 耗时: {}ms, requestId: {}", 
                    methodName, responseStr, executionTime, requestId);
        } catch (Exception e) {
            log.warn("解析响应结果异常, methodName: {}, requestId: {}, 异常信息: {}", 
                    methodName, requestId, e.getMessage());
        }
    }
    
    /**
     * 打印请求头信息
     */
    private void printHeadersLog(String requestId, LogLevel level) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 获取所有请求头
                Map<String, String> headersMap = new HashMap<>();
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    
                    // 敏感头信息处理
                    if (isSensitiveHeader(headerName)) {
                        headersMap.put(headerName, "******");
                    } else {
                        headersMap.put(headerName, headerValue);
                    }
                }
                
                logByLevel(level, "请求头信息: {}, requestId: {}", 
                        objectMapper.writeValueAsString(headersMap), requestId);
            }
        } catch (Exception e) {
            log.warn("解析请求头异常, requestId: {}, 异常信息: {}", requestId, e.getMessage());
        }
    }
    
    /**
     * 判断是否为敏感字段
     */
    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String lowerField = fieldName.toLowerCase();
        for (String sensitiveField : SENSITIVE_FIELDS) {
            if (lowerField.contains(sensitiveField)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否为敏感请求头
     */
    private boolean isSensitiveHeader(String headerName) {
        if (headerName == null) {
            return false;
        }
        
        String lowerHeader = headerName.toLowerCase();
        return lowerHeader.contains("authorization") || 
               lowerHeader.contains("cookie") || 
               lowerHeader.contains("token") || 
               lowerHeader.contains("secret") || 
               lowerHeader.contains("key");
    }
    
    /**
     * 对敏感信息进行脱敏处理
     */
    private String maskSensitiveInfo(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        return SensitiveUtil.maskSensitiveInfo(content);
    }
} 