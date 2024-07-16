package com.learn.flashsale.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("flash_sales")
public class FlashSales implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "flash_sale_id", type = IdType.AUTO)
    private Integer flashSaleId;

    @TableField("product_id")
    private Integer productId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;


}
