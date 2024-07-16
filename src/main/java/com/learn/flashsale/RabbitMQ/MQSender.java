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
//        CompletableFuture.supplyAsync(() -> rabbitTemplate.convertSendAndReceive("exchange1",
//                        "",
//                        jsonString))
//                .thenAccept(response -> {
//                    String re = String.valueOf(response);
//if (re.equals("success"))
//{
//    //可以userId查db给用户手机号发短信抢到了，或没抢到
//    System.out.println("用户id"+userIdFlashIdCmd.getUserId()+"抢到了");
////    String str= KeyProperties.DB_DECREASE+":"+userIdFlashIdCmd.getFlashSaleId();
////    rabbitTemplate.convertAndSend("exchange2", "", str);
//}
////    System.out.println("用户id"+userIdFlashIdCmd.getUserId()+"没抢到");
//
//                });
    }


//    public void UpdateDB(String str) {
////      String queueName = "queue1";
////      rabbitTemplate.convertAndSend(queueName,message);
//        //创建消息向queue1发送
//        rabbitTemplate.convertAndSend("exchange2", "", str);
//
//    }
}
