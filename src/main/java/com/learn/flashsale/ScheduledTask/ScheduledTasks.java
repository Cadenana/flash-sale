package com.learn.flashsale.ScheduledTask;

import com.learn.flashsale.domain.cmd.AddFlashSaleCmd;
import com.learn.flashsale.domain.po.FlashSales;
import com.learn.flashsale.mapper.FlashSalesMapper;
import com.learn.flashsale.propoties.KeyProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduledTasks {
//多个定时任务或将定时任务改为同步所有的秒杀商品库存
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;
    private FlashSales flashSale;
    @Autowired
    RedisTemplate<String,Object> redisTemplate;
    @Autowired
    FlashSalesMapper flashSalesMapper;

    public void startScheduledTask(FlashSales flashSale, Long time) {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
        }
        this.flashSale = flashSale;
        if (scheduledFuture == null || scheduledFuture.isCancelled()) {
            scheduledFuture = scheduler.scheduleAtFixedRate(this::executeTask, 0, 5, TimeUnit.SECONDS);
            // 设置任务在特定时间后关闭，
            scheduler.schedule(() -> scheduledFuture.cancel(true), time, TimeUnit.MINUTES);
        }
    }

    public void executeTask() {
        FlashSales flashSales = flashSalesMapper.selectById(flashSale.getFlashSaleId());

        Object quantity = redisTemplate.opsForValue().get(KeyProperties.FLASH_PREFIX + flashSale.getFlashSaleId());
        int quantityInt = Integer.parseInt(String.valueOf(quantity));
        flashSales.setQuantity(quantityInt);
        flashSalesMapper.updateById(flashSales);
        System.out.println("SpringBoot定时任务执行,更新剩余商品数量为"+quantity);
    }

    @PreDestroy
    public void cleanup() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
