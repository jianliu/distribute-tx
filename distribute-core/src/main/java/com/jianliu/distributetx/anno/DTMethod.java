package com.jianliu.distributetx.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 分布式注解,DTMethod达标的方法将被包装成sql入库，由异步任务执行具体操作
 */
@Target({TYPE, FIELD, METHOD})
@Retention(CLASS)
public @interface DTMethod {
    String value() default "";
}
