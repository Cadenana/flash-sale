package com.learn.flashsale.controller;
import com.learn.flashsale.Service.IProductsService;
import com.learn.flashsale.annotation.Idaccessannotation;
import com.learn.flashsale.domain.cmd.AddProductsCmd;

import com.learn.flashsale.propoties.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@RestController
@RequestMapping("/products")
public class ProductsController {
    @Autowired
    IProductsService productsService;
    @PostMapping("/addProduct")
//    @PreAuthorize("hasAnyAuthority('newGoods')")
    @PreAuthorize("hasAuthority('newGoods')")
    public Response addGoods(@RequestBody AddProductsCmd addProductsCmd)
    {
        return productsService.addGoods(addProductsCmd);
    }

    @PreAuthorize("hasAuthority('supplyGoods')")
    @PostMapping("/supplyGoods")
    public Response supplyGoods(@RequestParam Integer productId,@RequestParam Integer supplyNum)
    {
        return  productsService.supplyGoods(productId,supplyNum);
    }
    @Idaccessannotation()
    @PreAuthorize("hasAuthority('book')")
    @GetMapping("/getProductInfo")
    public Response getProductInfo(@RequestParam Integer id)
    {
        return productsService.getProductInfo(id);
    }

}
