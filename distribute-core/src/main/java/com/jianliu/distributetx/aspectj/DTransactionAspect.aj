package com.jianliu.distributetx.aspectj;

import com.jianliu.distributetx.repository.DBHelper;
import com.jianliu.distributetx.tx.DTContextHold;
import com.jianliu.distributetx.tx.invocation.MethodInvocation;
import com.jianliu.distributetx.util.TxUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 分布式注解Aspect切面定义
 */
@Aspect
public class DTransactionAspect {


    /**
     * 加execution(* *(..))的原因是防止执行两次
     * https://blog.csdn.net/u011116672/article/details/63685340
     */
    @Pointcut("execution(* *(..)) && @annotation(com.jianliu.distributetx.anno.DTransactional)")
    public void DTransactional() {
    }

    @Pointcut("execution(* *(..)) && @annotation(com.jianliu.distributetx.anno.DTMethod)")
    public void DTMethod() {
    }

    @Around("DTransactional()")
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        TxUtil.ensureContext();
        //调用原方法的执行。
        Object result = null;
        try {
            result = joinPoint.proceed();
            System.out.println("after run");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return result;
    }

    @Around("DTMethod()")
    public Object weaveJoinPointDtMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!DTContextHold.isInTransaction()) {
            return joinPoint.proceed();
        }

        //将入参保存到db中，不执行具体调用逻辑
        Class clazz = joinPoint.getThis().getClass();
        System.out.println(joinPoint.getThis());
        System.out.println(joinPoint.getTarget());
        Object[] params = joinPoint.getArgs();
        MethodInvocation methodInvocation = new MethodInvocation();
        methodInvocation.setClazz(clazz);
        methodInvocation.setParameters(params);
        methodInvocation.setMethod(joinPoint.getSignature().getName());
        DBHelper.addTask(methodInvocation, joinPoint.getTarget());

        return null;
    }

}