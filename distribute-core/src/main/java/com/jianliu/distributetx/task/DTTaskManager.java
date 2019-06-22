package com.jianliu.distributetx.task;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.FailureJobHandler;
import com.jianliu.distributetx.IllegalDTTaskException;
import com.jianliu.distributetx.config.DTConfiguration;
import com.jianliu.distributetx.config.DTGlobalConfig;
import com.jianliu.distributetx.entity.DTAppNode;
import com.jianliu.distributetx.entity.DTTask;
import com.jianliu.distributetx.repository.AppNodeRepository;
import com.jianliu.distributetx.repository.TaskRepository;
import com.jianliu.distributetx.serde.Serializer;
import com.jianliu.distributetx.tx.invocation.DTMethodInvocation;
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
public class DTTaskManager extends BaseLogger implements InitializingBean, ApplicationListener<ApplicationContextEvent>, ApplicationContextAware {

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
    private DTGlobalConfig globalTxConfig;

    @Resource
    private Serializer serializer;

    @Autowired(required = false)
    private FailureJobHandler failureJobHandler;

    /**
     * 非自动注入
     *
     * @see DTConfiguration
     */
    private DataSource dataSource;

    private ApplicationContext applicationContext;

    private JdbcTemplate jdbcTemplate;

    private volatile DTAppNode current;

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

    private Map<DTMethodInvocation, Method> methodCache = new ConcurrentHashMap<>();

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

        //app加载完成后启动任务
        ensureRegisterAppNode();

        if (event instanceof ContextRefreshedEvent) {
            logger.info("applicationContext加载结束，distribute_tx 心跳任务开启、异步扫描任务开启");
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

                List<DTTask> tasks = taskRepository.findNewTaskList(jdbcTemplate, globalTxConfig.getAppName(), lowerHashBound, upperHashBound);
                List<DTTask> retryTaskList = taskRepository.findRetryTaskList(jdbcTemplate, globalTxConfig.getAppName(), lowerHashBound, upperHashBound);
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
                    List<DTTask> failedTaskList = taskRepository.findFailedTaskList(jdbcTemplate, globalTxConfig.getAppName(), lowerHashBound, upperHashBound);
                    for (DTTask task : failedTaskList) {
                        DTMethodInvocation methodInvocation = getMethodInvocation(task);

                        if (methodInvocation == null) {
                            logger.warn("method invocation is null,app:{},txId:{}", task.getAppName(), task.getTxId());
                            return;
                        }

                        failureJobHandler.handle(methodInvocation);
                        int currentVersion = task.getVersion();
                        updateVersion(task);
                        task.setStatus(DTTask.STATUS_HANDLED);
                        taskRepository.updateStatus(jdbcTemplate, task, currentVersion);
                    }
                } catch (Exception e) {
                    logger.error("单次执行异常任务处理失败", e);
                }
            }
        }
    });

    private DTMethodInvocation getMethodInvocation(DTTask task) {
        byte[] taskDetail = task.getTaskDetail();
        DTMethodInvocation methodInvocation = null;
        try {
            methodInvocation = serializer.deserialize(taskDetail, DTMethodInvocation.class);
        } catch (Exception e) {
            logger.error("解析任务失败", e);
        }
        return methodInvocation;
    }

    private void runTasks(List<DTTask> tasks) {
        for (DTTask task : tasks) {
            try {
                runTask(task);
            } catch (IllegalDTTaskException e) {
                logger.error("任务已无效", e);
                task.setStatus(DTTask.STATUS_INVALID_TASK);
                taskRepository.updateStatus(jdbcTemplate, task, task.getVersion());
            } catch (Exception e) {
                logger.error("执行单个异步任务失败,{}-{}", task.getAppName(), task.getTxId(), e);
                task.setStatus(DTTask.STATUS_FAILURE);
                taskRepository.updateStatus(jdbcTemplate, task, task.getVersion());
            }
        }
    }

    /**
     * 执行任务
     *
     * @param task
     */
    private void runTask(DTTask task) {
        DTMethodInvocation methodInvocation = getMethodInvocation(task);
        if (logger.isDebugEnabled()) {
            logger.debug("即将开始之前dt异步任务，taskId:{},txId:{}", task.getId(), task.getTxId());
        }

        if (methodInvocation == null) {
            logger.warn("method invocation is null,app:{},txId:{}", task.getAppName(), task.getTxId());
            task.setStatus(DTTask.STATUS_INVALID_TASK);
            taskRepository.updateStatus(jdbcTemplate, task, task.getVersion());
            return;
        }
        Class beanClass = methodInvocation.getClazz();
        Object[] parameters = methodInvocation.getParameters();
        String methodName = methodInvocation.getMethod();

        Object bean = applicationContext.getBean(beanClass);
        if (bean == null) {
            logger.error("执行异步任务失败，bean不存在:{},txId:{}", beanClass, task.getTxId());
            throw new IllegalDTTaskException("bean不存在:" + beanClass);
        }

        Method targetMethod = methodCache.get(methodInvocation);
        if (targetMethod == null) {
            try {
                targetMethod = bean.getClass().getMethod(methodName, methodInvocation.getParameterTypes());
                methodCache.putIfAbsent(methodInvocation, targetMethod);
            } catch (NoSuchMethodException e) {
                logger.error("method:{}不存在,txId:{}", bean.getClass(), methodName, task.getTxId());
                throw new IllegalDTTaskException("method不存在:" + beanClass + "." + methodName);
            }
        }

        if (targetMethod == null) {
            logger.error("无效的method:{}.{},txId:{}", beanClass, methodName, task.getTxId());
            throw new IllegalDTTaskException("method不存在:" + beanClass + "." + methodName);
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
    public void executeTaskTransactional(DTTask task, Object[] parameters, Object bean, Method targetMethod) {
        task.setStatus(DTTask.STATUS_DOING);
        int currentVersion = task.getVersion();
        //利用版本号来防止多个进程重复执行任务导致状态变化不可控
        int result = taskRepository.updateStatus(jdbcTemplate, task, currentVersion);
        if (result == 1) {
            updateVersion(task);
            try {
                task.setTryTimes(task.getTryTimes() + 1);
                targetMethod.invoke(bean, parameters);
                task.setStatus(DTTask.STATUS_SUCCESS);
                if (logger.isDebugEnabled()) {
                    logger.debug("事务执行task成功，taskId:{},txId:{}", task.getId(), task.getTxId());
                }
                //taskRepository.updateStatus(jdbcTemplate, task, currentVersion);
                //任务执行成功，删除任务
                taskRepository.deleteById(jdbcTemplate, task);
                if (logger.isDebugEnabled()) {
                    logger.debug("事务删除已执行成功的任务,txId:{}", task.getTxId());
                }
            } catch (Exception e) {
                logger.error("appName-txId:{}-{}执行任务时系统异常", task.getAppName(), task.getTxId(), e);
                task.setStatus(DTTask.STATUS_FAILURE);
                //任务执行失败，记录状态
                taskRepository.updateStatus(jdbcTemplate, task, currentVersion);
                if (logger.isDebugEnabled()) {
                    logger.debug("任务执行失败,txId:{}", task.getTxId());
                }
            }
        }
    }

    private void updateVersion(DTTask task) {
        task.setVersion(task.getVersion() + 1);
    }

    private void ensureRegisterAppNode() {
        logger.info("开始ensureRegisterAppNode");
        String ip = globalTxConfig.getIp();

        DTAppNode appNode = appNodeRepository.findByIp(jdbcTemplate, globalTxConfig.getAppName(), ip);
        if (appNode != null) {
            appNode.setHashCode(globalTxConfig.getHashCode());
            if (DTAppNode.STATUS_OFFLINE.equals(appNode.getStatus())) {
                appNode.setStatus(DTAppNode.STATUS_ONLINE);
            }
            appNodeRepository.heartBeat(jdbcTemplate, appNode);
            current = appNode;
            return;
        }

        appNode = new DTAppNode();
        appNode.setCreateTime(new Date());
        appNode.setHashCode(globalTxConfig.getHashCode());
        appNode.setIp(globalTxConfig.getIp());
        appNode.setStatus(DTAppNode.STATUS_ONLINE);
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
            current.setStatus(DTAppNode.STATUS_ONLINE);
            current.setLastHeartBeatTime(new Date());
            appNodeRepository.heartBeat(jdbcTemplate, current);

            calHashBound();
        }
    }

    /**
     * 计算hash的上下界
     */
    private void calHashBound() {
        List<DTAppNode> appNodeList = appNodeRepository.findAvaibleList(jdbcTemplate, globalTxConfig.getAppName(),
                new Date(new Date().getTime() - AVAIBLE_TIME_BEFORE_CURRENT));
        int size = appNodeList.size();
        if (size == 0) {
            //正常情况不应该出现此类情况，至少本机是有效的
            logger.warn("appNodeList.size()==0 直接返回,异常情况，发现当前实例无正常心跳(DEBUG模式下是正常现象)");
            return;
        }

        Collections.sort(appNodeList, Comparator.comparing(DTAppNode::getHashCode));
        boolean hitCurrent = false;
        for (DTAppNode node : appNodeList) {
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
            DTAppNode node = appNodeList.get(i);
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
