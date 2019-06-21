package com.jianliu.distributetx.tx;

import java.util.UUID;

/**
 * distributetx
 *
 * @author jian.liu
 * @since 2019/6/17
 */
public class DTransactionContext {

    private String txId = UUID.randomUUID().toString();

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }
}
