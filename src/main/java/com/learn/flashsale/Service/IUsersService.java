package com.learn.flashsale.Service;

import com.learn.flashsale.domain.cmd.AddFlashSaleCmd;
import com.learn.flashsale.domain.cmd.UserRegisterCmd;
import com.learn.flashsale.domain.po.User;
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
public interface IUsersService extends IService<User> {


     Response register(UserRegisterCmd userRegisterCmd);

    Response login(String phone, String password);


    Response book(Integer productId,Integer quantity);

    Response ensure(Integer orderId);

    Response addFlashSale(AddFlashSaleCmd addFlashSaleCmd);

    Response getFlashSaleInfo(Integer flashSaleId);

    Response rob(Integer flashSaleId);
    Response robWithMysqlLock(Integer flashSaleId);
    Response robBaseOnQueue(Integer flashSaleId);

    Response getRobResult(Integer flashSaleId);

}
