package com.jianliu.distributetx.config;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.repository.DTDBHelper;
import com.jianliu.distributetx.serde.Serializer;
import com.jianliu.distributetx.serde.JacksonSerializer;
import com.jianliu.distributetx.repository.AppNodeRepository;
import com.jianliu.distributetx.repository.TaskRepository;
import com.jianliu.distributetx.task.OptimizeTableTask;
import com.jianliu.distributetx.task.DTTaskManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * class DistributeTxConfiguration
 *
 * @author jianliu
 * @since 2019/6/19
 */
@Configuration
//@ConditionalOnBean(DataSource.class)
public class DTConfiguration extends BaseLogger implements SchedulingConfigurer {

    private final static Serializer DEFAULT_SERIALIZER = new JacksonSerializer();

    @Value("${distribute.tx.serializer:}")
    private String encodeClass;

    @Resource
    private DataSource distributeTxDatasource;

    @Value("${distribute.tx.optimise.task.cron:}")
    private String cron;

    @Bean
    public DTTaskManager taskManager() {
        DTTaskManager taskManager = new DTTaskManager();
        taskManager.setDataSource(distributeTxDatasource);
        return taskManager;
    }

    @Bean
    public AppNodeRepository appNodeService() {
        return new AppNodeRepository();
    }

    @Bean
    public TaskRepository taskRepository() {
        return new TaskRepository();
    }

    @Bean
    public DTGlobalConfig globalTxConfig() {
        return new DTGlobalConfig();
    }

    @Bean
    public DTDBHelper dbHelper() {
        return new DTDBHelper();
    }

    @Bean
    public Serializer serializer() throws Throwable {
        if (StringUtils.isEmpty(encodeClass)) {
            return DEFAULT_SERIALIZER;
        }

        try {
            return (Serializer) this.getClass().getClassLoader().loadClass(encodeClass).newInstance();
        } catch (Throwable t) {
            logger.error("加载自定义Encoder失败", t);
            throw t;
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (!StringUtils.isEmpty(cron)) {
            taskRegistrar.setScheduler(taskExecutor());
            taskRegistrar.addTriggerTask(optimiseTableTask(), new CronTrigger(cron));
        }

    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(1);
    }

    @Bean
    public OptimizeTableTask optimiseTableTask() {
        OptimizeTableTask optimiseTableTask = new OptimizeTableTask();
        optimiseTableTask.setDataSource(distributeTxDatasource);
        return optimiseTableTask;
    }
}
