package com.learn.flashsale.Service.impl;

import com.learn.flashsale.domain.po.Orders;
import com.learn.flashsale.mapper.OrdersMapper;
import com.learn.flashsale.Service.IOrdersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersService {

}
