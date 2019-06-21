package com.jianliu.distributetx.task;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.config.DistributeTxConfiguration;
import com.jianliu.distributetx.config.GlobalTxConfig;
import com.jianliu.distributetx.entity.AppNode;
import com.jianliu.distributetx.repository.AppNodeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * class OptimizeTableTask
 * 仅共享表空间的表需要此操作
 * 生产环境一般都是开启了的独享表空间的,因此任务意义不大
 * <p>
 * show table status [like table_name]
 * InnoDB引擎的表分为独享表空间和同享表空间的表，我们可以通过SHOW VARIABLES LIKE 'innodb_file_per_table';来查看是否开启独享表空间。
 * <p>
 * 开启了独享表空间时是无法对表进行optimize操作的，如果操作，会返回Table does not support optimize, doing recreate + analyze instead。
 * 该结构下删除了大量的行，此时索引会重组并且会释放相应的空间因此不必优化。
 *
 * @author jianliu
 * @since 2019/6/20
 */
public class OptimizeTableTask extends BaseLogger implements Runnable {

    @Resource
    private AppNodeRepository appNodeRepository;

    @Resource
    private GlobalTxConfig globalTxConfig;

    private final static String SQL = "optimize table distx_task;";

    /**
     * 非自动注入
     *
     * @see DistributeTxConfiguration
     */
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run() {

        try {
            List<AppNode> appNodeList = appNodeRepository.findAvaibleList(jdbcTemplate,
                    globalTxConfig.getAppName(), new Date(new Date().getTime() - 5500));
            if (CollectionUtils.isEmpty(appNodeList)) {
                return;
            }

            //第一个节点执行定时任务
            AppNode appNode = appNodeList.get(0);
            if (appNode.getAppName().equals(globalTxConfig.getAppName()) && appNode.getIp().equals(globalTxConfig.getIp())
                    && appNode.getHashCode().equals(globalTxConfig.getHashCode())) {
                logger.warn("执行optimise table distx_task,本机配置:{}", globalTxConfig);
                Connection connection = DataSourceUtils.getConnection(dataSource);
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement(SQL);
                    logger.warn("optimise table distx_task; => result:{}", preparedStatement.executeUpdate());
                } catch (SQLException e) {
                    logger.error("执行表优化任务失败", e);
                }
            }
        } catch (Throwable t) {
            logger.error("执行表优化任务失败", t);
        }
    }
}
