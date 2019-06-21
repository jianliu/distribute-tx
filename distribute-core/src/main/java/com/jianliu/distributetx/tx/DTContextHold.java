package com.jianliu.distributetx.tx;

/**
 * distributetx
 *
 * @author jian.liu
 * @since 2019/6/17
 */
public class DTContextHold {

    private static ThreadLocal<DTransactionContext> contextThreadLocal = new ThreadLocal<>();

    public static ThreadLocal<DTransactionContext> getCurrentContextLocal() {
        return contextThreadLocal;
    }

    public static DTransactionContext getCurrentContext() {
        return contextThreadLocal.get();
    }


    public static boolean isInTransaction(){
        return contextThreadLocal.get() != null;
    }

}
