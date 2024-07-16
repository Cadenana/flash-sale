package com.learn.flashsale.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idaccessannotation {
//    限流key
    String key() default "limiter";
    /**
     * 指定时间
     */
    int second() default 60;
    /**
     * 指定时间内的访问次数
     */
    int maxCount() default 30;

}
