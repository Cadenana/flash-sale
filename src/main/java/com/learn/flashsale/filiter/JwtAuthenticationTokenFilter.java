package com.learn.flashsale.filiter;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.learn.flashsale.domain.po.UserDetail;
import com.learn.flashsale.exception.BizException;
import com.learn.flashsale.exception.ErrorCode;
import com.learn.flashsale.mapper.UsersMapper;
import com.learn.flashsale.propoties.KeyProperties;
import com.learn.flashsale.utils.IPUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.learn.flashsale.utils.JwtUtil;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @Author asiawu
 * @Date 2023-04-08 0:59
 * @Description: 放在所有过滤器前面，检查浏览器携带的token是否合法
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {


    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    UsersMapper usersMapper;

    @Autowired
    RedisTemplate redisTemplate;
    public static ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //todo hyperloglog实现用户访问量统计，但是只有本机一个ip没法测试
        //获取访问者的ip
        String ipAddress = IPUtil.getIpAddress(request);
//        System.out.println("访问的ip地址为:"+ipAddress);
        //将ip存入redis     opsForHyperLogLog()返回接口实例，允许对 Redis 中的 HyperLogLog 数据结构进行操作
        HyperLogLogOperations<String,String> hyperlog = redisTemplate.opsForHyperLogLog();
        hyperlog.add("cpp_bank_list_total_size_today",ipAddress);//创建HyperLogLog数据结构并把该ip加进去，后续加入就是向这个名字的HyperLogLog加ip
//        System.out.println(hyperlog.size("cpp_bank_list_total_size_today"));//获取该HyperLogLog的基数估计值


        //获取token
        String accessToken = request.getHeader(KeyProperties.TOKEN_HEADER);
        if (StringUtils.isEmpty(accessToken)) {
            filterChain.doFilter(request, response);
            //加return为了返回时第二次经过过滤器不再执行下面的代码
            return;
        }
        //解析token
        String userId = "";
        Claims claims = null;
        try {
            claims = JwtUtil.parseJWT(accessToken);
        } catch (Exception e) {
            throw new BizException(ErrorCode.TOKEN_PARSE_ERROR);
        }
        userId = claims.getSubject();
        //从redis获取用户信息
        String key = KeyProperties.TOKEN_PREFIX + userId;
        String userDetailStr = stringRedisTemplate.opsForValue().get(key);
        UserDetail userDetail = JSONObject.parseObject(userDetailStr, UserDetail.class);
        if (Objects.isNull(userDetail)) {
            throw new BizException(ErrorCode.TOKEN_PARSE_ERROR);
        }
        //存入SecurityContextHolder
        // 获取权限信息封装到authticationToken中
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        //存入ThreadLocal
        threadLocal.set(Integer.valueOf(userId));
        //放行
        filterChain.doFilter(request, response);
    }
}
