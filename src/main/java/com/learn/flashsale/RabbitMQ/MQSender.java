package com.learn.flashsale.RabbitMQ;
import com.alibaba.fastjson.JSON;
import com.learn.flashsale.propoties.KeyProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.learn.flashsale.domain.cmd.userIdFlashIdCmd;
import java.util.concurrent.CompletableFuture;

@Service
public class MQSender {

    @Autowired
    RabbitTemplate rabbitTemplate;
    //消息发送返回值一般为void,因为要异步
    public void sendOrderRequest(userIdFlashIdCmd userIdFlashIdCmd) {

        String jsonString = JSON.toJSONString(userIdFlashIdCmd);
        //创建消息向queue1发送
        rabbitTemplate.convertAndSend("exchange1",
                "",
                jsonString);
    }

    // 新增方法：向 queue2 的 exchange2 发送消息，供按序消费的队列处理
    public void sendToQueue2(userIdFlashIdCmd userIdFlashIdCmd) {
        String jsonString = JSON.toJSONString(userIdFlashIdCmd);
        rabbitTemplate.convertAndSend("exchange2", "", jsonString);
    }


//    public void UpdateDB(String str) {
////      String queueName = "queue1";
////      rabbitTemplate.convertAndSend(queueName,message);
//        //创建消息向queue1发送
//        rabbitTemplate.convertAndSend("exchange2", "", str);
//
//    }
}
