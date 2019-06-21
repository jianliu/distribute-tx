package com.jianliu.distributetx.task;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.FailureJobHandler;
import com.jianliu.distributetx.config.DistributeTxConfiguration;
import com.jianliu.distributetx.config.GlobalTxConfig;
import com.jianliu.distributetx.entity.AppNode;
import com.jianliu.distributetx.entity.Task;
import com.jianliu.distributetx.repository.AppNodeRepository;
import com.jianliu.distributetx.repository.TaskRepository;
import com.jianliu.distributetx.serde.Serializer;
import com.jianliu.distributetx.tx.invocation.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * distributetx
 * 管理定时任务
 *
 * @author jian.liu
 * @since 2019/6/18
 */
public class TaskManager extends BaseLogger implements InitializingBean, ApplicationListener<ApplicationContextEvent>, ApplicationContextAware {

    /**
     * 心跳;计算 lowerHashBound，upperHashBound间隔，单位ms
     */
    private final static int Heart_Beat_Inteval = 2000;
    /**
     * appNode如果在AVAIBLE_TIME_BEFORE_CURRENT ms内没有心跳，则当做已死亡
     */
    private final static int AVAIBLE_TIME_BEFORE_CURRENT = 5000;

    @Resource
    private AppNodeRepository appNodeRepository;

    @Resource
    private TaskRepository taskRepository;

    @Resource
    private GlobalTxConfig globalTxConfig;

    @Resource
    private Serializer serializer;

    @Autowired(required = false)
    private FailureJobHandler failureJobHandler;

    /**
     * 非自动注入
     *
     * @see DistributeTxConfiguration
     */
    private DataSource dataSource;

    private ApplicationContext applicationContext;

    private JdbcTemplate jdbcTemplate;

    private volatile AppNode current;

    /**
     * 本机查询任务时的低、高值
     * 不包含 excusive
     */
    private volatile long lowerHashBound;
    /**
     * 包含 incusive
     */
    private volatile long upperHashBound;

    private Timer timer = new Timer("distribute_tx_heartbeat_timer");

    /**
     * 每  Heart_Beat_Inteval 执行一次心跳以及计算 lowerHashBound，upperHashBound
     *
     * @see #Heart_Beat_Inteval
     * @see #AVAIBLE_TIME_BEFORE_CURRENT
     */
    private TimerTask timerTask = new HeartBeatTask();

    private Map<String, Method> methodCache = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {

        logger.info("event:{}", event);
        //app加载完成后启动任务
        ensureRegisterAppNode();

        if (event instanceof ContextRefreshedEvent) {
            logger.info("in event:{}", event);
            timer.schedule(timerTask, 2000, Heart_Beat_Inteval);
            //make sure run once
            calHashBound();
            scanTaskThread.start();
            if (failureJobHandler != null) {
                scanFailedThread.start();
            }
        }
    }

    private Thread scanTaskThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.error("break;", e);
                    break;
                }

                List<Task> tasks = taskRepository.findNewTaskList(jdbcTemplate, globalTxConfig.getAppName(), lowerHashBound, upperHashBound);
                List<Task> retryTaskList = taskRepository.findRetryTaskList(jdbcTemplate, globalTxConfig.getAppName(), lowerHashBound, upperHashBound);
                runTasks(tasks);
                runTasks(retryTaskList);
            }
        }
    });

    private Thread scanFailedThread = new Thread(new Runnable() {
        @Override
        public void run() {
            if (failureJobHandler == null) {
                return;
            }

            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error("break;", e);
                    break;
                }

                try {
                    List<Task> failedTaskList = taskRepository.findFailedTaskList(jdbcTemplate, globalTxConfig.getAppName(), lowerHashBound, upperHashBound);
                    for (Task task : failedTaskList) {
                        MethodInvocation methodInvocation = getMethodInvocation(task);

                        if (methodInvocation == null) {
                            logger.warn("method invocation is null,app:{},txId:{}", task.getAppName(), task.getTxId());
                            return;
                        }

                        failureJobHandler.handle(methodInvocation);
                        int currentVersion = task.getVersion();
                        updateVersion(task);
                        task.setStatus(Task.STATUS_HANDLED);
                        taskRepository.updateStatus(jdbcTemplate, task, currentVersion);
                    }
                } catch (Exception e) {
                    logger.error("单次执行异常任务处理失败", e);
                }
            }
        }
    });

    private MethodInvocation getMethodInvocation(Task task) {
        byte[] taskDetail = task.getTaskDetail();
        MethodInvocation methodInvocation = null;
        try {
            methodInvocation = serializer.deserialize(taskDetail, MethodInvocation.class);
        } catch (Exception e) {
            logger.error("解析任务失败", e);
        }
        return methodInvocation;
    }

    private void runTasks(List<Task> tasks) {
        for (Task task : tasks) {
            try {
                runTask(task);
            } catch (Exception e) {
                logger.error("执行单个异步任务失败,{}-{}", task.getAppName(), task.getTxId(), e);
            }
        }
    }

    /**
     * 执行任务
     *
     * @param task
     */
    private void runTask(Task task) {
        MethodInvocation methodInvocation = getMethodInvocation(task);

        if (methodInvocation == null) {
            logger.warn("method invocation is null,app:{},txId:{}", task.getAppName(), task.getTxId());
            return;
        }
        Class beanClass = methodInvocation.getClazz();
        Object[] parameters = methodInvocation.getParameters();
        String methodName = methodInvocation.getMethod();
        String methodCacheKey = methodInvocation.getClazz() + "-" + methodName;
        Object bean = applicationContext.getBean(beanClass);

        if (bean == null) {
            logger.error("执行异步任务失败，bean不存在:{}", beanClass);
            return;
        }

        Method targetMethod = methodCache.get(methodCacheKey);
        if (targetMethod == null) {
            for (Method method : bean.getClass().getSuperclass().getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    methodCache.putIfAbsent(methodCacheKey, method);
                    break;
                }
            }
        }

        if (targetMethod == null) {
            logger.error("无效的method:{}.{}", beanClass, methodName);
            return;
        }

        executeTaskTransactional(task, parameters, bean, targetMethod);
    }

    /**
     * 事务执行
     *
     * @param task
     * @param parameters
     * @param bean
     * @param targetMethod
     */
    @Transactional
    public void executeTaskTransactional(Task task, Object[] parameters, Object bean, Method targetMethod) {
        task.setStatus(Task.STATUS_DOING);
        int currentVersion = task.getVersion();
        //利用版本号来防止多个进程重复执行任务导致状态变化不可控
        int result = taskRepository.updateStatus(jdbcTemplate, task, currentVersion);
        if (result == 1) {
            updateVersion(task);
            try {
                task.setTryTimes(task.getTryTimes() + 1);
                targetMethod.invoke(bean, parameters);
                task.setStatus(Task.STATUS_SUCCESS);
                //taskRepository.updateStatus(jdbcTemplate, task, currentVersion);
                //任务执行成功，删除任务
                taskRepository.deleteById(jdbcTemplate, task);
                if (logger.isDebugEnabled()) {
                    logger.debug("删除已执行成功的任务,txId:{}", task.getTxId());
                }
            } catch (Exception e) {
                logger.error("appName-txId:{}-{}执行任务时系统异常", task.getAppName(), task.getTxId(), e);
                task.setStatus(Task.STATUS_FAILURE);
                //任务执行失败，记录状态
                taskRepository.updateStatus(jdbcTemplate, task, currentVersion);
                if (logger.isDebugEnabled()) {
                    logger.debug("任务执行失败,txId:{}", task.getTxId());
                }
            }
        }
    }

    private void updateVersion(Task task) {
        task.setVersion(task.getVersion() + 1);
    }


    private void ensureRegisterAppNode() {
        logger.info("开始ensureRegisterAppNode");
        String ip = globalTxConfig.getIp();

        AppNode appNode = appNodeRepository.findByIp(jdbcTemplate, globalTxConfig.getAppName(), ip);
        if (appNode != null) {
            if (AppNode.STATUS_OFFLINE.equals(appNode.getStatus())) {
                appNode.setStatus(AppNode.STATUS_ONLINE);
            }
            appNodeRepository.heartBeat(jdbcTemplate, appNode);
            current = appNode;
            return;
        }

        appNode = new AppNode();
        appNode.setCreateTime(new Date());
        appNode.setHashCode(globalTxConfig.getHashCode());
        appNode.setIp(globalTxConfig.getIp());
        appNode.setStatus(AppNode.STATUS_ONLINE);
        appNode.setLastHeartBeatTime(new Date());
        appNode.setAppName(globalTxConfig.getAppName());
        appNodeRepository.insert(jdbcTemplate, appNode);
        current = appNode;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    class HeartBeatTask extends TimerTask {

        public void run() {
            //心跳
            current.setStatus(AppNode.STATUS_ONLINE);
            current.setLastHeartBeatTime(new Date());
            appNodeRepository.heartBeat(jdbcTemplate, current);

            calHashBound();
        }
    }

    /**
     * 计算hash的上下界
     */
    private void calHashBound() {
        List<AppNode> appNodeList = appNodeRepository.findAvaibleList(jdbcTemplate, globalTxConfig.getAppName(),
                new Date(new Date().getTime() - AVAIBLE_TIME_BEFORE_CURRENT));
        int size = appNodeList.size();
        if (size == 0) {
            //正常情况不应该出现此类情况，至少本机是有效的
            logger.warn("appNodeList.size() == 0 直接返回 正常情况不应该出现此类情况，至少本机是有效的");
            return;
        }

        Collections.sort(appNodeList, Comparator.comparing(AppNode::getHashCode));
        boolean hitCurrent = false;
        for (AppNode node : appNodeList) {
            if (node.getId().equals(current.getId())) {
                hitCurrent = true;
            }
        }

        if (!hitCurrent) {
            //正常情况不应该出现此类情况，至少本机是有效的
            logger.warn("appNodeList没有本机节点，直接返回 正常情况不应该出现此类情况，至少本机是有效的");
            return;
        }
        //仅本机一个实例
        if (size == 1) {
            lowerHashBound = 0;
            upperHashBound = Long.MAX_VALUE;
            return;
        }

        for (int i = 0; i < size; i++) {
            AppNode node = appNodeList.get(i);
            if (node.getId().equals(current.getId())) {
                if (i == 0) {
                    //本机是第一个
                    lowerHashBound = 0;
                    upperHashBound = appNodeList.get(i + 1).getHashCode();
                } else if (i == size - 1) {
                    //本机是最后一个
                    lowerHashBound = appNodeList.get(i - 1).getHashCode();
                    upperHashBound = Long.MAX_VALUE;
                } else {
                    //中间
                    lowerHashBound = appNodeList.get(i - 1).getHashCode();
                    upperHashBound = node.getHashCode();
                }
            }
        }
    }


}
