package com.wujiuye.asyncframework;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 被注释的方法异步执行
 *
 * @author wujiuye
 * @version 1.0 on 2019/4/17
 */
@Target({ElementType.METHOD})
@Retention(RUNTIME)
@Documented
public @interface AsyncFunction {

}
