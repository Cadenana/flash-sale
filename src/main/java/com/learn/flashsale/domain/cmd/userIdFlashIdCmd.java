package com.learn.flashsale.domain.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class userIdFlashIdCmd  {
    private Integer userId;
    private Integer flashSaleId;
}
