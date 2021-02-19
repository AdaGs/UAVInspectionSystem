package com.wgx.sgcc.requestLimit.annotation;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Order(Ordered.HIGHEST_PRECEDENCE)
public @interface RequestLimit {
    /**
     * 允许访问的最大次数
     * @return
     */
    int count() default Integer.MAX_VALUE;

    /**
     * 时间段, 默认一分钟
     * @return
     */
    long time() default 60000;


}
