package com.jianliu.distributetx.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jianliu.distributetx.config.GlobalTxConfig;
import com.jianliu.distributetx.tx.DTContextHold;
import com.jianliu.distributetx.tx.DTransactionContext;
import com.jianliu.distributetx.tx.invocation.MethodInvocation;
import com.jianliu.distributetx.serde.Serializer;
import com.jianliu.distributetx.entity.Task;
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
public class DBHelper implements ApplicationContextAware {


    private static DBHelper instance;
    private static ApplicationContext applicationContext;
    private static Map<Object, String> beanNameCache = new ConcurrentHashMap<>();
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private TaskRepository taskRepository;
    @Resource
    private GlobalTxConfig globalTxConfig;
    @Resource
    private Serializer serializer;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DBHelper.applicationContext = applicationContext;
        instance = applicationContext.getBean(DBHelper.class);
    }

    public static void addTask(MethodInvocation invocation, Object bean) throws Exception {
        String beanName = "";
        Map<String, Object> beans = applicationContext.getBeansOfType(invocation.getClazz());
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (entry.getValue() == bean) {
                beanName = entry.getKey();
            }
        }

        instance.doAddTask(invocation);
    }

    private void doAddTask(MethodInvocation invocation) throws JsonProcessingException {
        DTransactionContext tx = DTContextHold.getCurrentContext();
        String txId = tx.getTxId();

        byte[] bytes = serializer.serialize(invocation);
        //获取Spring事务的当前连接，执行操作，jdbc template
        Task task = new Task();
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
