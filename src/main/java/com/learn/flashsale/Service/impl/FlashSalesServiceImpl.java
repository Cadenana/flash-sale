package com.learn.flashsale.Service.impl;

import com.learn.flashsale.RabbitMQ.MQSender;
import com.learn.flashsale.Service.IFlashSalesService;
import com.learn.flashsale.domain.po.FlashSales;
import com.learn.flashsale.mapper.FlashSalesMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.flashsale.propoties.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@Service
public class FlashSalesServiceImpl extends ServiceImpl<FlashSalesMapper, FlashSales> implements IFlashSalesService {
    @Autowired
    MQSender mqSender;
    @Autowired
    FlashSalesMapper flashSalesMapper;
    @Override
    public Response send(String msg) {
//        mqSender.sendOrderRequest(msg);
        return Response.success();
    }
}
