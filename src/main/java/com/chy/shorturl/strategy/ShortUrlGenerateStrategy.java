package com.chy.shorturl.strategy;

/**
 * 短链接生成策略接口
 *
 * @author Henry.Yu
 * @date 2023/07/11
 */
public interface ShortUrlGenerateStrategy {
    
    /**
     * 生成短链接编码
     *
     * @param url 原始URL
     * @param validator 短码验证器，用于检查短码是否已存在
     * @return 短链接编码
     */
    String generateShortUrl(String url, ShortCodeValidator validator);
    
    /**
     * 短码验证器函数式接口
     */
    @FunctionalInterface
    interface ShortCodeValidator {
        /**
         * 检查短码是否存在
         *
         * @param shortCode 短码
         * @return 是否存在，true表示存在，false表示不存在
         */
        boolean exists(String shortCode);
    }
} 