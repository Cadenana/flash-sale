package com.learn.flashsale.RabbitMQ;

import com.alibaba.fastjson.JSON;
import com.learn.flashsale.domain.cmd.userIdFlashIdCmd;
import com.learn.flashsale.domain.po.Orders;
import com.learn.flashsale.domain.po.Products;
import com.learn.flashsale.mapper.FlashSalesMapper;
import com.learn.flashsale.mapper.OrdersMapper;
import com.learn.flashsale.propoties.KeyProperties;
import org.redisson.api.RedissonClient;
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
    @Autowired
    RedissonClient redissonClient;
    //todo 感觉直接把订单相关信息传过来创建订单比较快？ 节约一个查询产品操作
    //必须查库存预检
    @Transactional
    @RabbitListener(queues = "queue1")
    public void listenSimpleQueue(String str) {
        //接收到消息，消息为秒杀活动，预检库存创建订单

        userIdFlashIdCmd userIdFlashIdCmd = JSON.parseObject(str, userIdFlashIdCmd.class);
        Integer flashSaleId = userIdFlashIdCmd.getFlashSaleId();
        Integer userId = userIdFlashIdCmd.getUserId();
        String productKey= KeyProperties.FLASH_PRODUCT+flashSaleId;
        String flashKey=KeyProperties.FLASH_PREFIX+flashSaleId;
        Object quantity = redisTemplate.opsForValue().get(flashKey);
        int quantityInt = Integer.parseInt(String.valueOf(quantity));
        String productInfo = stringRedisTemplate.opsForValue().get(productKey);
        if (--quantityInt<0) {
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

        System.out.println("用户"+userId+"的订单创建成功");

    }

    @Transactional
    @RabbitListener(queues = "queue2")
    public void listenQueue2(String str) {
        userIdFlashIdCmd cmd = JSON.parseObject(str, userIdFlashIdCmd.class);
        Integer flashSaleId = cmd.getFlashSaleId();
        Integer userId = cmd.getUserId();
        String flashKey = KeyProperties.FLASH_PREFIX + flashSaleId;
        String productKey = KeyProperties.FLASH_PRODUCT + flashSaleId;

        // 使用lua脚本原子扣减库存
        org.springframework.data.redis.core.script.DefaultRedisScript<Long> script = new org.springframework.data.redis.core.script.DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new org.springframework.scripting.support.ResourceScriptSource(new org.springframework.core.io.ClassPathResource("lua/decrGoodNum.lua")));
        Long result = redisTemplate.execute(script, java.util.Arrays.asList(flashKey));
        if (result == null || result == -1L) {
            System.out.println("[Queue2] 用户" + userId + "抢购失败，库存不足，活动id=" + flashSaleId);
            return;
        }

        Long expire = redisTemplate.getExpire(flashKey);
        if (expire == null) expire = 3600L;
        stringRedisTemplate.opsForValue().set(flashSaleId + ":" + userId, "booked", expire, java.util.concurrent.TimeUnit.SECONDS);

        // 创建订单
        String productInfo = stringRedisTemplate.opsForValue().get(productKey);
        Products product = JSON.parseObject(productInfo, Products.class);
        Orders order = new Orders();
        order.setOrderStatus(false);
        order.setFlashSaleId(flashSaleId);
        order.setProductId(product == null ? null : product.getProductId());
        order.setQuantity(1);
        order.setCreatedAt(LocalDateTime.now());
        order.setUserId(userId);
        ordersMapper.insert(order);

        System.out.println("[Queue2] 用户" + userId + "抢购成功，订单已创建，活动id=" + flashSaleId + ", 订单id=" + order.getOrderId());
    }

}
