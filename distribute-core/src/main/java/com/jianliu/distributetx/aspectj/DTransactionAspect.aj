package com.jianliu.distributetx.aspectj;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.repository.DTDBHelper;
import com.jianliu.distributetx.tx.DTContextHold;
import com.jianliu.distributetx.tx.invocation.DTMethodInvocation;
import com.jianliu.distributetx.util.TxUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * 分布式注解Aspect切面定义
 */
@Aspect
public class DTransactionAspect extends BaseLogger {


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
            if (logger.isDebugEnabled()) {
                logger.debug("即将执行DTransactional注解拦截");
            }
            result = joinPoint.proceed();
            if (logger.isDebugEnabled()) {
                logger.debug("完成执行DTransactional注解拦截");
            }
        } catch (Throwable t) {
            logger.error("拦截DTransactional失败", t);
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
        DTMethodInvocation methodInvocation = new DTMethodInvocation();
        methodInvocation.setClazz(clazz);
        methodInvocation.setParameters(params);
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        methodInvocation.setMethod(methodSignature.getName());
        methodInvocation.setParameterTypes(methodSignature.getParameterTypes());
        if (logger.isDebugEnabled()) {
            logger.debug("拦截DTMethod注解，即将插入任务");
        }
        DTDBHelper.addTask(methodInvocation, joinPoint.getTarget());

        return null;
    }

}