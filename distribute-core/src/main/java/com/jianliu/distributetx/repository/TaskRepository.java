package com.jianliu.distributetx.repository;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.entity.DTTask;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;


/**
 * distributetx
 *
 * @author jian.liu
 * @since 2019/6/18
 */
public class TaskRepository extends BaseLogger {

    private TaskMapper taskMapper = new TaskMapper();

    public void insert(JdbcTemplate jdbcTemplate, DTTask task) {

        String sql = "INSERT INTO `distx_task` (`hash_code`,`status`,`task_detail`, `try_times`,`tx_id`,`consumer_time`,`create_time`,`app_name`,`version`) " +
                "VALUES(?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, task.getHashCode(), task.getStatus(),
                new ByteArrayInputStream(task.getTaskDetail()),
                task.getTryTimes(), task.getTxId(), task.getConsumerTime(), task.getCreateTime(), task.getAppName(), 1);

        Integer id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID() as value", Integer.class);
        task.setId(id);
        if (logger.isDebugEnabled()) {
            logger.debug("添加任务，任务id:{}", id);
        }
    }

    public List<DTTask> findNewTaskList(JdbcTemplate jdbcTemplate, String appName, long lowerHash, long upperHash) {
        //SingleColumnRowMapper.mapRow
        String sql = "select * from distx_task where app_name =? and hash_code > ? and hash_code <= ? and status = 1 limit 20 ";
        return jdbcTemplate.query(sql, new Object[]{appName, lowerHash, upperHash}, taskMapper);
    }

    public List<DTTask> findRetryTaskList(JdbcTemplate jdbcTemplate, String appName, long lowerHash, long upperHash) {
        //SingleColumnRowMapper.mapRow
        String sql = "select * from distx_task where app_name =? and hash_code > ? and hash_code <= ? and status = 4 and try_times < 5 limit 20 ";
        return jdbcTemplate.query(sql, new Object[]{appName, lowerHash, upperHash}, taskMapper);
    }

    public List<DTTask> findFailedTaskList(JdbcTemplate jdbcTemplate, String appName, long lowerHash, long upperHash) {
        //SingleColumnRowMapper.mapRow
        String sql = "select * from distx_task where app_name =? and hash_code > ? and hash_code <= ? and status = 4 and try_times >= 5 limit 20 ";
        return jdbcTemplate.query(sql, new Object[]{appName, lowerHash, upperHash}, taskMapper);
    }

    public int updateStatus(JdbcTemplate jdbcTemplate, DTTask task, int currentVersion) {
        String sql = "update distx_task set status = ?,consumer_time=?,version = ?,try_times=? where id = ? and  version = ?";
        return jdbcTemplate.update(sql, task.getStatus(), new Date(), task.getVersion(), task.getTryTimes(), task.getId(), currentVersion);
    }

    public int deleteById(JdbcTemplate jdbcTemplate, DTTask task) {
        String sql = "delete from  distx_task  where id = ? ";
        return jdbcTemplate.update(sql, task.getId());
    }


    static class TaskMapper implements RowMapper<DTTask> {

        public DTTask mapRow(ResultSet rs, int rowNum) throws SQLException {
            DTTask task = new DTTask();
            task.setId(rs.getInt("id"));
            task.setAppName(rs.getString("app_name"));
            task.setHashCode(rs.getLong("hash_code"));
            task.setStatus(rs.getInt("status"));
            Blob blob = rs.getBlob("task_detail");
            task.setTaskDetail(blob.getBytes(1, (int) blob.length()));
            task.setTryTimes(rs.getInt("try_times"));
            task.setConsumerTime(rs.getDate("consumer_time"));
            task.setCreateTime(rs.getDate("create_time"));
            task.setTxId(rs.getString("tx_id"));
            task.setVersion(rs.getInt("version"));
            return task;
        }

    }

}
