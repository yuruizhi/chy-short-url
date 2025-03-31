package com.chy.shorturl.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感信息处理工具类
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
public class SensitiveUtil {

    /**
     * 手机号正则表达式
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}");
    
    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
    
    /**
     * 身份证号正则表达式
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[1-2]\\d|3[0-1])\\d{3}[0-9Xx]");
    
    /**
     * 银行卡号正则表达式（简化版）
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");

    private SensitiveUtil() {
        // 工具类禁止实例化
    }
    
    /**
     * 对敏感信息进行脱敏处理
     *
     * @param content 需要脱敏的内容
     * @return 脱敏后的内容
     */
    public static String maskSensitiveInfo(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        content = maskPhone(content);
        content = maskEmail(content);
        content = maskIdCard(content);
        content = maskBankCard(content);
        
        return content;
    }
    
    /**
     * 手机号脱敏（保留前3位和后4位）
     *
     * @param content 内容
     * @return 脱敏后的内容
     */
    public static String maskPhone(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        Matcher matcher = PHONE_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String phone = matcher.group();
            if (phone.length() >= 11) {
                String maskedPhone = phone.substring(0, 3) + "****" + phone.substring(7);
                matcher.appendReplacement(result, maskedPhone);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 邮箱脱敏（保留首字符和@之后的部分）
     *
     * @param content 内容
     * @return 脱敏后的内容
     */
    public static String maskEmail(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        Matcher matcher = EMAIL_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String email = matcher.group();
            int atIndex = email.indexOf('@');
            if (atIndex > 1) {
                String maskedEmail = email.charAt(0) + "****" + email.substring(atIndex);
                matcher.appendReplacement(result, maskedEmail);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 身份证号脱敏（保留前6位和后4位）
     *
     * @param content 内容
     * @return 脱敏后的内容
     */
    public static String maskIdCard(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        Matcher matcher = ID_CARD_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String idCard = matcher.group();
            if (idCard.length() >= 15) {
                String maskedIdCard = idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
                matcher.appendReplacement(result, maskedIdCard);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 银行卡号脱敏（保留前6位和后4位）
     *
     * @param content 内容
     * @return 脱敏后的内容
     */
    public static String maskBankCard(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        Matcher matcher = BANK_CARD_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String cardNo = matcher.group();
            if (cardNo.length() >= 10) {
                String maskedCardNo = cardNo.substring(0, 6) + "****" + cardNo.substring(cardNo.length() - 4);
                matcher.appendReplacement(result, maskedCardNo);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
} 