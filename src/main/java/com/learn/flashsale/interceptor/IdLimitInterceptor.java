package com.learn.flashsale.interceptor;

import cn.hutool.core.util.ObjUtil;
import com.learn.flashsale.annotation.Idaccessannotation;
import com.learn.flashsale.filiter.JwtAuthenticationTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

@Component
public class IdLimitInterceptor implements HandlerInterceptor {
    @Autowired
    StringRedisTemplate redisTemplate;


    private DefaultRedisScript<Long> getRedisScript;


    //初始化脚本
    @PostConstruct
    private void init() {
        getRedisScript = new DefaultRedisScript<>();
        getRedisScript.setResultType(Long.class);
        getRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/IdAccess.lua")));
        System.out.println("lua脚本初始化完成");
    }
    //拦截器处理

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {

            //判断HandlerMethod实例，转换为HandlerMethod
            if (handler instanceof HandlerMethod) {
                //获取注解，没有放行
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                Idaccessannotation idAnnotation = handlerMethod.getMethodAnnotation(Idaccessannotation.class);
                if (null == idAnnotation) {
                    return true;
                }
                //获取注解参数，获取id已访问接口次数
                int seconds = idAnnotation.second();
                String key = idAnnotation.key();
                int maxCount = idAnnotation.maxCount();
                Integer id = JwtAuthenticationTokenFilter.threadLocal.get();
                String servletPath = request.getServletPath();
                String redisKey = key + "id:" + id + servletPath;
                String o = redisTemplate.opsForValue().get(redisKey);
                if (ObjUtil.isEmpty(o)) {
                    System.out.println(redisKey + "访问次数为" + 0);
                } else {
                    int count = Integer.parseInt(o);
                    if (count>=maxCount)
                    {
                        System.out.println("访问过度了");
                        return false;
                    }
                    System.out.println(redisKey + "访问次数为" + o);
                }
                //执行lua脚本
                ArrayList<String> keyList = new ArrayList<>();
                keyList.add(redisKey);
                Long res = redisTemplate.execute(getRedisScript, keyList, String.valueOf(seconds), String.valueOf(maxCount));
                if (res != null && res == 0) {
                    System.out.println("过度访问");
                    return false;
                }
                return true;
            }
        }

        catch (Exception e) {
            System.out.println(e.toString());
            System.out.println("id限流异常");
        }
return true;
    }
}



