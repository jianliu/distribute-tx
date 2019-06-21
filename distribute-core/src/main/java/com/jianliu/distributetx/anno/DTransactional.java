package com.jianliu.distributetx.anno;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 分布式注解,继承Transactional，开启Spring事务后，DTransactional将自动继承Spring的事务机制
 */
@Target({TYPE, FIELD, METHOD})
@Retention(RUNTIME)
@Transactional
@Inherited
public @interface DTransactional {
}
