技术架构：SpringBoot+SpringMVC+SpringSecurity+Mybatis-plus+Redis+RabbitMQ

项目描述：一个以秒杀为主题的服务端，

用户：下单，抢购

商家：上架补充商品，用自家商品创建秒杀活动

管理员：定义了，没啥用

1.登陆注册

基于SpringSecurity实现RBCA权限模型，基于注解限制接口调用权限，实现单用户多身份，，自定义过滤器实现jwt鉴权

2.缓存一致性

常规信息采用更新mysql删除redis的形式，秒杀服务则优先更新redis，mysql异步同步

3.日用户访问量统计

从过滤器链获取用户ip，使用hyperLogLog实现日用户访问量统计（条件原因无法测试）

4.秒杀性能优化

创建秒杀活动时将活动信息存入redis，并设置过期时间，key过期即为活动结束，用户抢单用rabbitMQ异步创建订单并优先更新redis剩余数量，再通过回调函数异步同步到mysql

5.布隆过滤器

整合Redission实现布隆过滤器解决缓存穿透问题，随机化key过期时间防止缓存雪崩

6.一人一单

mysql订单表建立userid和flashSale联合索引，下单前进行订单查询，保证一人一单。









