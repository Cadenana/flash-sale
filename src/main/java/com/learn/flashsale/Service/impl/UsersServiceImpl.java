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
import lombok.extern.log4j.Log4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import com.learn.flashsale.domain.cmd.userIdFlashIdCmd;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

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

    DefaultRedisScript<Long> redisScript;
    @PostConstruct
    public void init() {
        List<User> list = this.list();
        bloomFilter = bloomFilterUtil.create("UserIdFilter", expectedInsertions, falseProbability);
        for (User user : list) {
            bloomFilter.add(Long.valueOf(user.getPhone()));
        }
        redisScript=new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/decrGoodNum.lua")));
        System.out.println("预减库存lua脚本初始化完成");
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
        System.out.println("用户" + user.getUserId() + "注册成功，权限有：" + roles.toString());
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
        System.out.println("用户" + userId + "登录，权限有：" + permissions.toString());
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
        System.out.println("用户" + userid + "下单成功，订单id为：" + order.getOrderId());
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
        System.out.println("商家" + JwtAuthenticationTokenFilter.threadLocal.get() + "创建秒杀活动，活动id为：" + flashSale.getFlashSaleId());
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
        System.out.println("用户" + JwtAuthenticationTokenFilter.threadLocal.get() + "查询秒杀活动，活动id为：" + flashSaleId);
        return Response.of(map);
    }

    //优先更新redis，db异步更新
    //抢购逻辑可以优化，redis用lua脚本执行扣减库存
    //使用mq异步执行订单创建等操作，采用分布式事务或补偿机制保证库存扣减和订单创建一致性
    @Override
    public Response rob(Integer flashSaleId) {
        //redis查询秒杀活动，不存在或没库存则已结束
        Integer userId = JwtAuthenticationTokenFilter.threadLocal.get();
        String flashKey = KeyProperties.FLASH_PREFIX + flashSaleId;
        Long expire = redisTemplate.getExpire(flashKey);
        RLock lock = redissonClient.getLock(KeyProperties.LockPrefix + userId + ":" + flashSaleId);
        try {
            if (expire==null||expire<=0) {
                return Response.of("活动不存在或已结束");
            }
            //分布式锁，保证同一时间只能由一个该id用户的账号执行抢购逻辑
            lock.lock();
            if (stringRedisTemplate.opsForValue().get(flashSaleId+":"+userId)!=null) {
                return Response.of("不可重复抢购");
            }
            Long execute = redisTemplate.execute(redisScript, Arrays.asList(flashKey));
            if(execute == -1) {
                return Response.of("秒杀商品已被抢购完");
            }
            else {
                stringRedisTemplate.opsForValue().set(flashSaleId+":"+userId, "booked", expire, TimeUnit.SECONDS);
                //请求由rabbitMQ异步操作，监听队列收到消息预减库存
                userIdFlashIdCmd userIdFlashIdCmd = new userIdFlashIdCmd(userId, flashSaleId);
                //发送异步请求，返回结果，已抢购，创建订单中
                mqSender.sendOrderRequest(userIdFlashIdCmd);
                System.out.println("用户" + userId + "抢购秒杀活动，活动id为：" + flashSaleId + "，结果：请求已发送，创建订单中");
                return Response.of("抢购成功,订单创建后请及时支付");
            }
        }
        catch (Exception e) {
            return Response.of("不可重复抢购");
        }
        finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }
    //安全的，但业务逻辑耦合到 DB，不推荐。
    @Override
    @Transactional
    public Response robWithMysqlLock(Integer flashSaleId) {
        FlashSales flashSale = flashSalesMapper.selectForUpdate(flashSaleId);
        if (flashSale == null || flashSale.getQuantity() <= 0) {
            System.out.println("mysql抢购失败");
            return Response.error("400", "活动不存在或已结束");
        }else {
            flashSale.setQuantity(flashSale.getQuantity() - 1);
            int updated  = flashSalesMapper.updateById(flashSale);
            if (updated==0 ) {
                System.out.println("手慢了，库存不足");
                return Response.error("400", "活动不存在或已结束");
            }
            // 创建订单逻辑
            Integer userId = JwtAuthenticationTokenFilter.threadLocal.get();
            Orders order = new Orders();
            order.setOrderStatus(false);
            order.setFlashSaleId(flashSaleId);
            order.setProductId(flashSale.getProductId());
            order.setQuantity(1);
            order.setCreatedAt(LocalDateTime.now());
            order.setUserId(userId);
            ordersMapper.insert(order);
            System.out.println("mysql抢购用户" + userId + "抢购秒杀活动，活动id为：" + flashSaleId + "，结果：抢购成功");
            return Response.of("抢购成功");
        }
    }

    //将秒杀库存提前加载到Redis中，用户请求进入消息队列，后端服务按序处理。
    public Response robBaseOnQueue(Integer flashSaleId) {
        Integer userId = JwtAuthenticationTokenFilter.threadLocal.get();
        String flashKey = KeyProperties.FLASH_PREFIX + flashSaleId;
        Long expire = redisTemplate.getExpire(flashKey);

        if (expire == null || expire <= 0) {
            return Response.of("活动不存在或已结束");
        }

        // 检查用户是否已经抢购过
        if (stringRedisTemplate.opsForValue().get(flashSaleId + ":" + userId) != null) {
            return Response.of("不可重复抢购");
        }

        // 将请求发送到专用的队列，由消费端按序处理（消费端负责redis扣减库存并创建订单）
        userIdFlashIdCmd cmd = new userIdFlashIdCmd(userId, flashSaleId);
        mqSender.sendToQueue2(cmd);

        System.out.println("用户" + userId + "抢购秒杀活动，活动id为：" + flashSaleId + "，请求已发送到队列，等待处理");
        return Response.of("抢购请求已提交，请等待结果");
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
        List<Orders> orders = ordersMapper.selectList(wrapper);
        //orders非空
        if (orders == null || orders.isEmpty()) {
            System.out.println("用户" + userId + "查询秒杀结果，活动id为：" + flashSaleId + "，结果：秒杀中");
            return Response.of("秒杀中，请稍后查询结果");
        }
        Orders order1 = orders.get(0);
        HashMap<String, String> map = new HashMap<>();
        if (!ObjectUtil.isEmpty(order1)) {
            map.put("success", "秒杀成功，请及时付款");
            map.put("order", JSON.toJSONString(order1));
            System.out.println("用户" + userId + "查询秒杀结果，活动id为：" + flashSaleId + "，结果：秒杀成功");
            return Response.of(map);
        } else {
            redisTemplate.delete(KeyProperties.LockPrefix+userId);
            System.out.println("用户" + userId + "查询秒杀结果，活动id为：" + flashSaleId + "，结果：秒杀失败");
            return Response.of("很遗憾,秒杀失败");
        }
    }


    public List<String> getRoleByIds(Integer id) {
        return usersMapper.getRoleByIds(id);
    }


}
