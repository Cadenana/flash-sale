package com.learn.flashsale.domain.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;



@Data
@AllArgsConstructor
public class AddProductsCmd {


    private String productName;

    private Integer quantity;

    private Boolean status;


}