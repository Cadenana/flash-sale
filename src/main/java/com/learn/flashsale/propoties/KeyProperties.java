package com.learn.flashsale.propoties;

/**
 * @Author aihei
 * @Description: 所有有关key的参数
 */
public class KeyProperties {

    //Token相关
    public static final String TOKEN_PREFIX = "token:";
    public static final String TOKEN_HEADER = "Authorization";

    //秒杀相关
    public static final String FLASH_PREFIX="FlashSale:";
    public static final String FLASH_PRODUCT="FlashProduct:";



      public static final String DB_INCREASE="Increase";
    public static final String DB_DECREASE ="Decrease";
//锁相关
    public static final  String LockPrefix="Lock:";
    public static final  String DistributedLock="DisLock:";




    //存位置信息
    public static final String LOCATION_ORDER_PREFIX = "location:order:";
    public static final String LOCATION_USER_KEY = "user";
    public static final String LOCATION_RIDER_KEY = "goodsSeller";
    public static final String LOCATION_RIDER_AVAILABLE_KEY = "isAvailable";

    //权限相关的key
    public static final String ROLE_USER_KEY = "user";
    public static final String ROLE_SHOPKEEPER_KEY = "goodsSeller";
    public static final String ROLE_ADMIN_KEY = "admin";

}
