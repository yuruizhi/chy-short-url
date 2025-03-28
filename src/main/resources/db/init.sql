-- 创建数据库
CREATE DATABASE IF NOT EXISTS `chy_short_url` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `chy_short_url`;

-- 创建URL映射表
CREATE TABLE IF NOT EXISTS `url_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `original_url` varchar(2048) NOT NULL COMMENT '原始URL',
  `short_code` varchar(16) NOT NULL COMMENT '短码',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `access_count` bigint(20) NOT NULL DEFAULT '0' COMMENT '访问次数',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_short_code` (`short_code`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='URL映射表'; 