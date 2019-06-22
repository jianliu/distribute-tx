package com.jianliu.distributetx.tx.invocation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * distributetx
 * 方法执行需要的信息
 *
 * @author jian.liu
 * @since 2019/6/17
 */
public class DTMethodInvocation implements Serializable {

    private static final long serialVersionUID = 42L;

    private Class clazz;

    private String method;

    private Class[] parameterTypes;

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

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DTMethodInvocation that = (DTMethodInvocation) o;
        return Objects.equals(clazz, that.clazz) &&
                Objects.equals(method, that.method) &&
                Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(clazz, method);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }
}
