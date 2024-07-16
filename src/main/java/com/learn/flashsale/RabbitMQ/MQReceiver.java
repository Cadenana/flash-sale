package com.learn.flashsale.RabbitMQ;

import com.alibaba.fastjson.JSON;
import com.learn.flashsale.domain.cmd.userIdFlashIdCmd;
import com.learn.flashsale.domain.po.Orders;
import com.learn.flashsale.domain.po.Products;
import com.learn.flashsale.mapper.FlashSalesMapper;
import com.learn.flashsale.mapper.OrdersMapper;
import com.learn.flashsale.propoties.KeyProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MQReceiver {
    @Autowired
    OrdersMapper ordersMapper;

    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    FlashSalesMapper flashSalesMapper;
    //todo 感觉直接把订单相关信息传过来创建订单比较快？ 节约一个查询产品操作
    //必须查库存预检
    @Transactional
    @RabbitListener(queues = "queue1")
    public void listenSimpleQueue(String str) {
        //接收到消息，消息为秒杀活动，预检库存创建订单
//        byte[] body = message.getBody();
//        String s = new String(body);
//        System.out.println(s);
//        String[] split = s.split(",");
        userIdFlashIdCmd userIdFlashIdCmd = JSON.parseObject(str, userIdFlashIdCmd.class);
        Integer flashSaleId = userIdFlashIdCmd.getFlashSaleId();
        Integer userId = userIdFlashIdCmd.getUserId();
        String productKey= KeyProperties.FLASH_PRODUCT+flashSaleId;
        String flashKey=KeyProperties.FLASH_PREFIX+flashSaleId;
        Object quantity = redisTemplate.opsForValue().get(flashKey);
        int quantityInt = Integer.parseInt(String.valueOf(quantity));
        String productInfo = stringRedisTemplate.opsForValue().get(productKey);

        if (--quantityInt<0)
        {
            return;
        }
        Products product = JSON.parseObject(productInfo, Products.class);
        Orders order = new Orders();
        order.setOrderStatus(false);
        order.setFlashSaleId(flashSaleId);
        order.setProductId(product.getProductId());
        order.setQuantity(1);
        order.setCreatedAt(LocalDateTime.now());
        order.setUserId(userId);
        ordersMapper.insert(order);
        redisTemplate.opsForValue().decrement(flashKey);
        System.out.println("用户"+userId+"抢到了");
//        return "success";
    }


//    @Transactional
//    @RabbitListener(queues = "queue2")
//    public void listenSimpleQueue2(String str) {
//        String[] split = str.split(":");
//        Integer flashSaleId = Integer.valueOf(split[1]);
//        if (split[0].equals(KeyProperties.DB_DECREASE))
//{
//    FlashSales flashSales = flashSalesMapper.selectById(flashSaleId);
//    flashSales.setQuantity(flashSales.getQuantity()-1);
//    flashSalesMapper.updateById(flashSales);
//}
//else
//{
//    FlashSales flashSales = flashSalesMapper.selectById(flashSaleId);
//    flashSales.setQuantity(flashSales.getQuantity()+1);
//    flashSalesMapper.updateById(flashSales);
//}
//    }
}
