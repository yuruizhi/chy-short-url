package com.chy.shorturl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * URL映射实体类
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("url_mapping")
public class UrlMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 原始URL
     */
    private String originalUrl;

    /**
     * 短码
     */
    private String shortCode;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 访问次数
     */
    private Long accessCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除 0-未删除 1-已删除
     */
    private Integer isDeleted;
} 