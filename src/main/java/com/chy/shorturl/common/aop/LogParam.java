package com.chy.shorturl.common.aop;

import java.lang.annotation.*;

/**
 * 日志参数注解，用于自动打印方法入参和出参
 *
 * @author Henry.Yu
 * @date 2024/04/27
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogParam {
    
    /**
     * 是否打印请求参数，默认为true
     */
    boolean printRequest() default true;
    
    /**
     * 是否打印响应结果，默认为false
     */
    boolean printResponse() default false;
    
    /**
     * 日志描述，可用于标识接口用途
     */
    String desc() default "";
    
    /**
     * 是否打印请求头信息，默认为false
     */
    boolean printHeaders() default false;
    
    /**
     * 是否打印异常堆栈，默认为true
     */
    boolean printException() default true;
    
    /**
     * 是否隐藏敏感信息，默认为false
     * 当为true时，密码、手机号等敏感信息将被脱敏处理
     */
    boolean hideSensitive() default false;
    
    /**
     * 响应结果最大长度，超过此长度将被截断
     * 默认为0，表示不限制长度
     */
    int responseMaxLength() default 0;
    
    /**
     * 日志级别，默认为INFO
     */
    LogLevel level() default LogLevel.INFO;
    
    /**
     * 日志级别枚举
     */
    enum LogLevel {
        /**
         * DEBUG级别
         */
        DEBUG,
        
        /**
         * INFO级别
         */
        INFO,
        
        /**
         * WARN级别
         */
        WARN
    }
} 