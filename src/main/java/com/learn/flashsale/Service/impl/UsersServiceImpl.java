package com.learn.flashsale.Service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.flashsale.RabbitMQ.MQSender;
import com.learn.flashsale.ScheduledTask.ScheduledTasks;
import com.learn.flashsale.domain.cmd.AddFlashSaleCmd;
import com.learn.flashsale.domain.cmd.UserRegisterCmd;
import com.learn.flashsale.domain.po.*;
import com.learn.flashsale.filiter.JwtAuthenticationTokenFilter;
import com.learn.flashsale.mapper.*;
import com.learn.flashsale.Service.IUsersService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.flashsale.propoties.KeyProperties;
import com.learn.flashsale.propoties.Response;
import com.learn.flashsale.utils.BloomFilterUtil;
import com.learn.flashsale.utils.JwtUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.learn.flashsale.domain.cmd.userIdFlashIdCmd;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, User> implements IUsersService {
    @Autowired
    UsersMapper usersMapper;
    @Autowired
    RolesMapper rolesMapper;
    @Autowired
    UserRolesMapper userRolesMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    ProductsMapper productsMapper;
    @Autowired
    FlashSalesMapper flashSalesMapper;
    @Autowired
    OrdersMapper ordersMapper;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    @Autowired
    MQSender mqSender;
    @Autowired
    ScheduledTasks scheduledTask;
    public static final long WEB_JWT_EXPIRE_TIME = 24 * 60 * 60;
    public static final long WEB_JWT_EXPIRE_TIME2 = 1000 * 60 * 60 * 24;
    // 预期插入数量
    static long expectedInsertions = 6000L;
    // 误判率
    static double falseProbability = 0.01;
    // 非法请求所返回的JSON
    static String illegalJson = "[\"com\",{\"id\":null,\"productName\":\"null\",\"\":null,\"\":null}]";
    private RBloomFilter<Long> bloomFilter = null;
    @Resource
    private BloomFilterUtil bloomFilterUtil;
    @Autowired
    RedissonClient redissonClient;

    @PostConstruct
    public void init() {
        List<User> list = this.list();
        bloomFilter = bloomFilterUtil.create("UserIdFilter", expectedInsertions, falseProbability);
        for (User user : list) {
            bloomFilter.add(Long.valueOf(user.getPhone()));
        }
    }


    @Override
    public Response register(UserRegisterCmd userRegisterCmd) {
        List<User> users = usersMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getPhone, userRegisterCmd.getPhone()));
        if (!ObjectUtil.isEmpty(users)) {
            return Response.error("400", "该手机号已注册");
        }
        User user = new User();
        BeanUtils.copyProperties(userRegisterCmd, user);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        List<Integer> roles = userRegisterCmd.getRoles();
        for (int i = 0; i < roles.size(); i++) {
            LambdaQueryWrapper<Roles> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Roles::getRoleId, roles.get(i));
            Roles role = rolesMapper.selectOne(wrapper);
            if (ObjectUtil.isEmpty(roles)) {
                return Response.error("400", "权限参数错误，1用户，2商家，3管理员");
            }
        }
        usersMapper.insert(user);
        for (int i = 0; i < roles.size(); i++) {
            userRolesMapper.insert(new UserRoles(user.getUserId(), roles.get(i)));
        }
        bloomFilter.add(Long.valueOf(userRegisterCmd.getPhone()));
        return Response.success();
    }

    @Override
    public Response login(String phone, String password) {
        if (!bloomFilter.contains(Long.valueOf(phone))) {
            return Response.error("400", "滚去注册");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = usersMapper.selectOne(wrapper);
        if (ObjectUtil.isEmpty(user)) {
            return Response.error("400", "用户不存在");
        }
        if (!user.getPassword().equals(password)) {
            return Response.error("400", "用户存在密码错误老弟");
        }
        user.setLastLoginAt(LocalDateTime.now());
        usersMapper.updateById(user);
        Integer userId = user.getUserId();
        HashMap<String, Object> map = new HashMap<>();
        String key = KeyProperties.TOKEN_PREFIX + userId;
        List<String> permissions = usersMapper.getPermissionsById(userId);
        List<String> roles = getRoleByIds(userId);
        String prefix = "ROLE_";
        for (String role : roles) {
            permissions.add(prefix + role);
        }
        UserDetail userDetail = new UserDetail(user, permissions);
        String value = JSON.toJSONString(userDetail);
        stringRedisTemplate.opsForValue()
                .set(key, value, WEB_JWT_EXPIRE_TIME, TimeUnit.SECONDS);
        String token = JwtUtil.createJWT(String.valueOf(userId), WEB_JWT_EXPIRE_TIME2);
        map.put("token", token);
        return Response.of(map);
    }


    @Override
    public Response book(Integer productId, Integer quantity) {
        Products products = productsMapper.selectById(productId);
        if (products.getQuantity() < quantity) {
            return Response.error("400", "库存不足");
        }
//创建订单
        Orders order = new Orders();
        order.setCreatedAt(LocalDateTime.now());
        Integer userid = JwtAuthenticationTokenFilter.threadLocal.get();
        order.setUserId(userid);
        order.setOrderStatus(false); //默认未付款
        order.setQuantity(quantity);     //
        order.setFlashSaleId(0); //只是下单不是秒杀活动
        order.setProductId(productId);
        ordersMapper.insert(order);

        //更新库存

        int newQuantity = products.getQuantity() - quantity;
        products.setQuantity(newQuantity);
        products.setUpdatedAt(LocalDateTime.now());
        productsMapper.updateById(products);
        return Response.of(order);
    }

    @Override
    public Response ensure(Integer orderId) {
        Orders orders = ordersMapper.selectById(orderId);
        if (ObjectUtil.isEmpty(orders))
            return Response.error("400", "订单不存在");
        orders.setOrderStatus(true);
        ordersMapper.updateById(orders);
        return Response.success();

    }

    //创建秒杀活动
    //现在开始，持续时长为redis设置的过期时间
    @Override
    public Response addFlashSale(AddFlashSaleCmd addFlashSaleCmd) {
        //产品库存判断
        Products products = productsMapper.selectById(addFlashSaleCmd.getProductId());
        if (ObjectUtil.isEmpty(products)) {
            return Response.error("400", "商品不存在");
        }
        if (products.getSellerId() != JwtAuthenticationTokenFilter.threadLocal.get() || products.getQuantity() < addFlashSaleCmd.getQuantity()) {
            return Response.error("400", "商品不属于您或库存不足");
        }
        //创建秒杀活动
        FlashSales flashSale = new FlashSales();
        flashSale.setQuantity(addFlashSaleCmd.getQuantity());
        flashSale.setProductId(addFlashSaleCmd.getProductId());
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(addFlashSaleCmd.getContinueTime());
        flashSale.setEndTime(endTime);
        flashSale.setStartTime(startTime);
        //更新原商品库存
        products.setQuantity(products.getQuantity() - addFlashSaleCmd.getQuantity());
        products.setUpdatedAt(LocalDateTime.now());
        productsMapper.updateById(products);
        //秒杀活动信息存mysql
        flashSalesMapper.insert(flashSale);
        //秒杀活动剩余商品存入redis，秒杀产品信息也要存
        String keyProduct = KeyProperties.FLASH_PRODUCT + flashSale.getFlashSaleId();
        String valueProduct = JSON.toJSONString(products);
        String keyFlashSale = KeyProperties.FLASH_PREFIX + flashSale.getFlashSaleId();
        Integer valueFlashSale = flashSale.getQuantity();
        Long continueTime = addFlashSaleCmd.getContinueTime();
        stringRedisTemplate.opsForValue().set(keyProduct, valueProduct, continueTime, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(keyFlashSale, valueFlashSale, continueTime, TimeUnit.MINUTES);
        scheduledTask.startScheduledTask(flashSale, addFlashSaleCmd.getContinueTime());
        return Response.success();
    }

    @Override
    public Response getFlashSaleInfo(Integer flashSaleId) {
        String productKey = KeyProperties.FLASH_PRODUCT + flashSaleId;
        String flashKey = KeyProperties.FLASH_PREFIX + flashSaleId;
        Long expire = redisTemplate.getExpire(flashKey);
        Object quantity = redisTemplate.opsForValue().get(flashKey);
        String productInfo = stringRedisTemplate.opsForValue().get(productKey);
        if (ObjectUtil.isEmpty(productInfo)) {
            return Response.error("400", "秒杀活动已结束或不存在");
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("剩余数量", String.valueOf(quantity));
        map.put("剩余时间", String.valueOf(expire));
        map.put("秒杀产品信息", productInfo);
        return Response.of(map);
    }

    //优先更新redis，db异步更新
    @Override
    public Response rob(Integer flashSaleId) {
        //redis查询秒杀活动，不存在或没库存则已结束
        String flashKey = KeyProperties.FLASH_PREFIX + flashSaleId;
        Object quantity = redisTemplate.opsForValue().get(flashKey);
        Long expire = redisTemplate.getExpire(flashKey);
        if (ObjectUtil.isEmpty(quantity)) {
            return Response.error("400", "活动已结束");
        }
        int quantityInt = Integer.parseInt(String.valueOf(quantity));
        if (quantityInt <= 0) {
            return Response.error("400", "秒杀商品已被抢购完");
        }
        Integer userId = JwtAuthenticationTokenFilter.threadLocal.get();
//数据库订单表创建了秒杀用户id联合索引，查订单存在拒绝，一个用户只能抢一次
//        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>().eq(Orders::getFlashSaleId, flashSaleId).eq(Orders::getUserId, userId);
//        //todo 还是查db了，性能不好
//        List<Orders> orders = ordersMapper.selectList(wrapper);
//        if (!ObjectUtil.isEmpty(orders)) {
//            return Response.error("400", "本次秒杀活动不可重复参与");
//        }
        RLock rLock = redissonClient.getLock(KeyProperties.DistributedLock + userId);
        try {

            boolean res = rLock.tryLock(10, 10, TimeUnit.SECONDS);
            if (res) {
                //请求由rabbitMQ异步操作，监听队列收到消息预减库存      创建订单减库存事务级别
                userIdFlashIdCmd userIdFlashIdCmd = new userIdFlashIdCmd(userId, flashSaleId);
//发送异步请求，返回结果，正在抢购
                mqSender.sendOrderRequest(userIdFlashIdCmd);
                stringRedisTemplate.opsForValue().set(KeyProperties.LockPrefix+userId,userId.toString(),expire,TimeUnit.SECONDS);
                return Response.of("正在抢购，请稍后");
                //后续获得结果时查询订单，存在就是抢到了，MQ回调函数根据结果异步更新mysql
            }
        }
        catch (Exception e) {
            throw new RuntimeException("aquire lock fail");
        } finally {
            //无论如何, 最后都要解锁
            rLock.unlock();
        }
        return Response.of("不可重复抢购");
    }

    @Override
    public Response getRobResult(Integer flashSaleId) {
        try {
            // 休眠2秒（2000毫秒）
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // 处理中断异常
            e.printStackTrace();
        }
        Integer userId = JwtAuthenticationTokenFilter.threadLocal.get();
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>().eq(Orders::getFlashSaleId, flashSaleId).eq(Orders::getUserId, userId);
        Orders order1 = ordersMapper.selectOne(wrapper);
        HashMap<String, String> map = new HashMap<>();
        if (!ObjectUtil.isEmpty(order1)) {
            map.put("success", "秒杀成功，请及时付款");
            map.put("order", JSON.toJSONString(order1));
            return Response.of(map);
        } else {
            redisTemplate.delete(KeyProperties.LockPrefix+userId);
            return Response.of("很遗憾,秒杀失败");
        }
    }


    public List<String> getRoleByIds(Integer id) {
        return usersMapper.getRoleByIds(id);
    }


}
