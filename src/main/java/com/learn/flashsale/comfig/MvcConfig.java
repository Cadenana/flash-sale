package com.learn.flashsale.comfig;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.flashsale.interceptor.IdLimitInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class MvcConfig implements WebMvcConfigurer {
    @Autowired
    IdLimitInterceptor interceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(interceptor)
                .addPathPatterns("/products/getProductInfo");
    }


//    @Override
//    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
//        for (HttpMessageConverter<?> converter : converters) {
//            //解决配置文件中spring.jackson.date-format=...在添加拦截器后失效问题
//            //允许传来的数字以 0 开头（解析订单号后四位）
//            if (converter instanceof MappingJackson2HttpMessageConverter) {
//                MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
//                ObjectMapper objectMapper = jsonConverter.getObjectMapper();
//                objectMapper
//                        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
//                        .setTimeZone(TimeZone.getTimeZone("GMT-8"))
//                        .configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature(), true);
//                break;
//            }
//        }
//    }





}
