package com.learn.flashsale.controller;


import com.learn.flashsale.Service.IUsersService;
import com.learn.flashsale.domain.cmd.UserRegisterCmd;
import com.learn.flashsale.propoties.Response;
import com.sun.istack.internal.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.learn.flashsale.domain.cmd.AddFlashSaleCmd;
/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@RestController
@RequestMapping("/users")
public class UsersController {
    @Autowired
    IUsersService usersService;

@PostMapping("/register")
    public Response register(@RequestBody UserRegisterCmd userRegisterCmd) {
    return usersService.register(userRegisterCmd);
}

@PostMapping("/login")
    public Response login(@NotNull @RequestParam String phone,@NotNull @RequestParam String password) {
    return usersService.login(phone,password);
}


    @PreAuthorize("hasAuthority('book')")
@PostMapping("/book")
    public Response book(@RequestParam Integer productId,@RequestParam Integer quantity) {
        return usersService.book(productId,quantity);
    }
    @PreAuthorize("hasRole('user')")
    @PostMapping("/ensure")
    public Response ensure(Integer orderId)
    {
        return usersService.ensure(orderId);
    }

    @PreAuthorize("hasRole('business')")
    @PostMapping("/addFlashSale")
    public Response addFlashSale(@RequestBody AddFlashSaleCmd addFlashSaleCmd) {
        return usersService.addFlashSale(addFlashSaleCmd);
    }


@PreAuthorize("hasAuthority('rob')")
    @GetMapping("/getFlashSaleInfo")
    public Response getFlashSaleInfo(@RequestParam Integer FlashSaleId) {
    return usersService.getFlashSaleInfo(FlashSaleId);
}

    @PreAuthorize("hasAuthority('rob')")
   @PostMapping("/rob")
    public Response rob(@RequestParam Integer FlashSaleId) {
        return usersService.rob(FlashSaleId);
    }
    //robWithMysqlLock
    @PreAuthorize("hasAuthority('rob')")
    @PostMapping("/robWithMysqlLock")
    public Response robWithMysqlLock(@RequestParam Integer FlashSaleId) {
        return usersService.robWithMysqlLock(FlashSaleId);
    }
    @PreAuthorize("hasAuthority('rob')")
    @PostMapping("/robBaseOnQueue")
    public Response robBaseOnQueue(@RequestParam Integer FlashSaleId) {
        return usersService.robBaseOnQueue(FlashSaleId);
    }

    @PreAuthorize("hasAuthority('rob')")
    @GetMapping("/getRobResult")
    public Response getRobResult(@RequestParam Integer FlashSaleId)
    {
        return usersService.getRobResult(FlashSaleId);
    }
}
