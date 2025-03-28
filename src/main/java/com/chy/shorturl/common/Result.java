package com.chy.shorturl.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回结果
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private T data;

    /**
     * 成功结果
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /**
     * 成功结果
     *
     * @param message 消息
     * @param data    数据
     * @param <T>     数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 错误结果
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误结果
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /**
     * 错误结果
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误结果
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
} 