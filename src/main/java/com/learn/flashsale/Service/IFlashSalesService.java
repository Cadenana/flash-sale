package com.learn.flashsale.Service;

import com.learn.flashsale.domain.po.FlashSales;
import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.flashsale.propoties.Response;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
public interface IFlashSalesService extends IService<FlashSales> {

    Response send(String msg);

}
