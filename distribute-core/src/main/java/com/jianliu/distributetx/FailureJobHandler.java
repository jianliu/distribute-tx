package com.jianliu.distributetx;

import com.jianliu.distributetx.tx.invocation.DTMethodInvocation;

/**
 * class FailJobHandler
 * 执行5次后仍然失败的任务处理器
 *
 * @author jianliu
 * @since 2019/6/20
 */
public interface FailureJobHandler {

    /**
     * 失败任务处理器
     * @param invocation
     */
    void handle(DTMethodInvocation invocation);

}
