package com.learn.flashsale.controller;


import com.learn.flashsale.RabbitMQ.MQSender;
import com.learn.flashsale.Service.IFlashSalesService;
import com.learn.flashsale.propoties.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@RestController
@RequestMapping("/flash-sales")
public class FlashSalesController {
    @Autowired
    IFlashSalesService flashSalesService;


    @PostMapping("/send")
    public Response send(@RequestParam  String msg)
    {
        return flashSalesService.send(msg);
    }

}
