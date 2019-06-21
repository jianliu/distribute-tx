package com.jianliu.distributetx.tx.invocation;

import java.io.Serializable;

/**
 * distributetx
 *
 * @author jian.liu
 * @since 2019/6/17
 */
public class MethodInvocation implements Serializable {

    private static final long serialVersionUID = 42L;

    private Class clazz;

    private String method;

    private Object[] parameters;

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}
