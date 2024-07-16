package com.learn.flashsale.comfig;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Random;

@EnableCaching
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private String port;

//    @Value("${spring.redis.password}")
//    private String password;    redis没有设置密码则无需配置密码


    @Bean(destroyMethod = "shutdown")  // bean销毁时关闭Redisson实例，但不关闭Redis服务
    public RedissonClient redisson() {
        //创建配置
        Config config = new Config();
        /**
         *  连接哨兵：config.useSentinelServers().setMasterName("myMaster").addSentinelAddress()
         *  连接集群： config.useClusterServers().addNodeAddress()
         */
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
//                .setPassword(password)
                .setTimeout(5000);
        //根据config创建出RedissonClient实例
        return Redisson.create(config);
    }

    //todo 这段配置不是很明白，好像是redis序列化相关，
    //之前的 jackson2JsonRedisSerializer序列化localDate报错，改用fastjson了
    @Bean
    public CacheManager RedisCacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> redisSerializer = new StringRedisSerializer();
        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<>(Object.class);

        // 配置 FastJson 序列化特性
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        fastJsonRedisSerializer.getFastJsonConfig().setSerializerFeatures(
                SerializerFeature.WriteClassName,
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.PrettyFormat
        );

        // 配置序列化解决乱码的问题
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 设置缓存过期时间  为解决缓存雪崩,所以将过期时间加随机值
                .entryTtl(Duration.ofSeconds(60 * 60 + new Random().nextInt(60 * 10)))
                // 设置key的序列化方式
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer))
                // 设置value的序列化方式
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(fastJsonRedisSerializer));
        // .disableCachingNullValues(); //为防止缓存击穿，所以允许缓存null值
        RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                // 启用RedisCache以将缓存 put/evict 操作与正在进行的 Spring 管理的事务同步
                .transactionAware()
                .build();
        return cacheManager;
    }
}
