package com.learn.flashsale.domain.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddFlashSaleCmd {
    private Integer productId;

    private Integer quantity;

    private  Long continueTime;

}
