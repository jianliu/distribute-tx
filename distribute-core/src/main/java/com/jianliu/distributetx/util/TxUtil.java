package com.jianliu.distributetx.util;

import com.jianliu.distributetx.tx.DTContextHold;
import com.jianliu.distributetx.tx.DTransactionContext;

/**
 * distributetx
 *
 * @author jian.liu
 * @since 2019/6/18
 */
public class TxUtil {

    public static ThreadLocal<DTransactionContext> ensureContext() {
        ThreadLocal<DTransactionContext> contextThreadLocal = DTContextHold.getCurrentContextLocal();
        if (contextThreadLocal.get() == null) {
            contextThreadLocal.set(new DTransactionContext());
        }
        return contextThreadLocal;
    }


}
