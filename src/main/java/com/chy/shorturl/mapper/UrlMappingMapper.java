package com.chy.shorturl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chy.shorturl.entity.UrlMapping;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * URL映射Mapper接口
 *
 * @author Henry.Yu
 * @date 2025/03/28
 */
public interface UrlMappingMapper extends BaseMapper<UrlMapping> {

    /**
     * 根据短码查询原始URL
     *
     * @param shortCode 短码
     * @return 原始URL对象
     */
    @Select("SELECT * FROM url_mapping WHERE short_code = #{shortCode} AND is_deleted = 0")
    UrlMapping findByShortCode(@Param("shortCode") String shortCode);

    /**
     * 更新访问次数
     *
     * @param id 主键ID
     * @return 影响行数
     */
    @Update("UPDATE url_mapping SET access_count = access_count + 1 WHERE id = #{id}")
    int incrementAccessCount(@Param("id") Long id);
}