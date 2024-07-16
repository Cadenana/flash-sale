package com.learn.flashsale.Service;

import com.learn.flashsale.domain.cmd.AddProductsCmd;
import com.learn.flashsale.domain.po.Products;
import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.flashsale.propoties.Response;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
public interface IProductsService extends IService<Products> {

    Response addGoods(AddProductsCmd addProductsCmd);

    Response supplyGoods(Integer productId,Integer supplyNum);

    Response getProductInfo(Integer id);
}
