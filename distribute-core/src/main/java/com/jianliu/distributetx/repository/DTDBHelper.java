package com.jianliu.distributetx.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jianliu.distributetx.config.DTGlobalConfig;
import com.jianliu.distributetx.tx.DTContextHold;
import com.jianliu.distributetx.tx.DTransactionContext;
import com.jianliu.distributetx.tx.invocation.DTMethodInvocation;
import com.jianliu.distributetx.serde.Serializer;
import com.jianliu.distributetx.entity.DTTask;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * distributetx
 *
 * @author jian.liu
 * @since 2019/6/17
 */
public class DTDBHelper implements ApplicationContextAware {


    private static DTDBHelper INSTANCE;
    private static ApplicationContext applicationContext;
    private static Map<Object, String> beanNameCache = new ConcurrentHashMap<>();
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private TaskRepository taskRepository;
    @Resource
    private DTGlobalConfig globalTxConfig;
    @Resource
    private Serializer serializer;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DTDBHelper.applicationContext = applicationContext;
        INSTANCE = applicationContext.getBean(DTDBHelper.class);
    }

    public static void addTask(DTMethodInvocation invocation, Object bean) throws Exception {
        INSTANCE.doAddTask(invocation);
    }

    private void doAddTask(DTMethodInvocation invocation) throws JsonProcessingException {
        DTransactionContext tx = DTContextHold.getCurrentContext();
        String txId = tx.getTxId();

        byte[] bytes = serializer.serialize(invocation);
        //获取Spring事务的当前连接，执行操作，jdbc template
        DTTask task = new DTTask();
        task.setAppName(globalTxConfig.getAppName());
        task.setHashCode(globalTxConfig.getHashCode());
        task.setStatus(1);
        task.setTaskDetail(bytes);
        task.setTryTimes(0);
        task.setTxId(txId);
        task.setConsumerTime(null);
        task.setCreateTime(new Date());

        taskRepository.insert(jdbcTemplate, task);
    }


}
