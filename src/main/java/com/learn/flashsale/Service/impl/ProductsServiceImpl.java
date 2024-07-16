package com.learn.flashsale.Service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.learn.flashsale.domain.cmd.AddProductsCmd;
import com.learn.flashsale.domain.po.Products;
import com.learn.flashsale.filiter.JwtAuthenticationTokenFilter;
import com.learn.flashsale.mapper.ProductsMapper;
import com.learn.flashsale.Service.IProductsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.flashsale.propoties.Response;
import com.learn.flashsale.utils.BloomFilterUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@Service
public class ProductsServiceImpl extends ServiceImpl<ProductsMapper, Products> implements IProductsService {
@Autowired
ProductsMapper productsMapper;
    // 预期插入数量
    static long expectedInsertions = 200L;
    // 误判率
    static double falseProbability = 0.01;
    // 非法请求所返回的JSON
    static String illegalJson = "[\"com\",{\"id\":null,\"productName\":\"null\",\"\":null,\"\":null}]";
    private RBloomFilter<Long> bloomFilter = null;
    @Resource
    private BloomFilterUtil bloomFilterUtil;
    @Resource
    private RedissonClient redissonClient;

    @PostConstruct // 项目启动的时候执行该方法，也可以理解为在spring容器初始化的时候执行该方法
    public void init() {
        // 启动项目时初始化bloomFilter
        List<Products> productsList = this.list();
//        List<Products> productsList = productsMapper.selectList(new LambdaQueryWrapper<Products>());
        bloomFilter = bloomFilterUtil.create("productIdWhiteList", expectedInsertions, falseProbability);
        for (Products products : productsList) {
            bloomFilter.add(Long.valueOf(products.getProductId()));
        }
    }



    @Override
    public Response addGoods(AddProductsCmd addProductsCmd) {
        Products product = new Products();
        BeanUtils.copyProperties(addProductsCmd,product);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setSellerId(JwtAuthenticationTokenFilter.threadLocal.get());
        productsMapper.insert(product);
        bloomFilter.add(Long.valueOf(product.getProductId())); //id加入布隆过滤器，否则被拦截了
        return Response.success();
    }


    @Override
    @CacheEvict(cacheNames = "products",key="#productId")//供给后删除缓存保证数据库缓存一致性
    public Response supplyGoods(Integer productId,Integer supplyNum) {
        Products products = productsMapper.selectById(productId);
        Integer userid = JwtAuthenticationTokenFilter.threadLocal.get();
        if (products.getSellerId()!=userid)
        {
            return Response.error("400","不是您的商品，无法补给");
        }
        int num = products.getQuantity() + supplyNum;
        products.setQuantity(num);
        int i = productsMapper.updateById(products);
        return i==1? Response.success():Response.error();
    }

    @Cacheable(cacheNames = "products", key = "#id", unless = "#result==null")
    @Override
    public Response getProductInfo(Integer id) {
        if (!bloomFilter.contains(Long.valueOf(id)))
        {
            return Response.error("400","想缓存穿透？做梦");
        }
        Products products = productsMapper.selectById(id);
        if (ObjectUtil.isEmpty(products))
        {
            return Response.error("400","商品不存在");
        }
        return Response.of(products);
    }
}
