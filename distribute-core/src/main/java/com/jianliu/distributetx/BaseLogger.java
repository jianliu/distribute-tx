package com.jianliu.distributetx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class BaseLogger
 *
 * @author jianliu
 * @since 2019/6/19
 */
public abstract class BaseLogger {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

}
