package com.learn.flashsale.domain.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddProductsCmd {


    private String productName;

    private Integer quantity;

    private Boolean status;


}