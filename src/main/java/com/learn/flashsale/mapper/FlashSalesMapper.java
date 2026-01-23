package com.learn.flashsale.mapper;

import com.learn.flashsale.domain.po.FlashSales;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@Mapper
public interface FlashSalesMapper extends BaseMapper<FlashSales> {
    @Select("SELECT * FROM flash_sales WHERE flash_sale_id = #{flashSaleId} FOR UPDATE")
    FlashSales selectForUpdate(@Param("flashSaleId") Integer flashSaleId);
}
