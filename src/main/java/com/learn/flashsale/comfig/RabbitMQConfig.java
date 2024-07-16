package com.learn.flashsale.comfig;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//MQ组件定义中方法名为队列或交换机名
//@Qualifier指定交换机或队列名实现绑定
@Configuration
public class RabbitMQConfig {

    // 定义队列
    @Bean
    public Queue queue1() {
        return new Queue("queue1");
    }
    // 定义交换机
    @Bean
    public FanoutExchange exchange1() {
        return new FanoutExchange("exchange1");
    }
    //使用@Qualifier确保交换机的正确绑定
    @Bean
    public Binding binding(@Qualifier("queue1") Queue exampleQueue, @Qualifier("exchange1") FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(exampleQueue).to(fanoutExchange);
    }



//    @Bean
//    public Queue queue2() {
//        return new Queue("queue2");
//    }
//    // 定义交换机
//    @Bean
//    public FanoutExchange exchange2() {
//        return new FanoutExchange("exchange2");
//    }
//    //使用@Qualifier确保交换机的正确绑定
//    @Bean
//    public Binding binding1(@Qualifier("queue2") Queue exampleQueue, @Qualifier("exchange2") FanoutExchange fanoutExchange) {
//        return BindingBuilder.bind(exampleQueue).to(fanoutExchange);
//    }









//    // 定义TopicExchange
//    @Bean
//    public TopicExchange topicExchange() {
//        return new TopicExchange("example.topic");
//    }
//
//    // 定义多个队列
//
//    @Bean
//    public Queue queue2() {
//        return new Queue("queue2");
//    }
//
//    @Bean
//    public Queue queue3() {
//        return new Queue("queue3");
//    }
//
//
//
//    // 定义复杂的绑定关系
//    @Bean
//    public Binding binding1(Queue queue3, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queue3).to(topicExchange).with("routing.key1");
//    }
//
//    @Bean
//    public Binding binding2(Queue queue2, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queue2).to(topicExchange).with("routing.key2");
//    }
//
//    @Bean
//    public Binding binding3(Queue queue3, TopicExchange topicExchange) {
//        return BindingBuilder.bind(queue3).to(topicExchange).with("common.#");
//    }





}