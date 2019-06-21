package com.jianliu.distributetx.repository;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.anno.DTMethod;
import com.jianliu.distributetx.anno.DTransactional;
import com.jianliu.distributetx.entity.AppNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.CollectionUtils;

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
public class AppNodeRepository extends BaseLogger {

    private AppNodeMapper appNodeMapper = new AppNodeMapper();

    public void heartBeat(JdbcTemplate jdbcTemplate, AppNode appNode) {
        String sql = "update distx_app_node set status = ?,last_heartbeat_time=? where id = ?";
        jdbcTemplate.update(sql, appNode.getStatus(), appNode.getLastHeartBeatTime(), appNode.getId());
    }

    public AppNode findByIp(JdbcTemplate jdbcTemplate, String appName, String ip) {
        String sql = "select * from distx_app_node where app_name =? and ip=? limit 1";
        List<AppNode> appNodeList = jdbcTemplate.query(sql, new Object[]{appName, ip}, appNodeMapper);
        if (!CollectionUtils.isEmpty(appNodeList)) {
            return appNodeList.get(0);
        }
        return null;
    }

    public List<AppNode> findList(JdbcTemplate jdbcTemplate, String appName) {
        //SingleColumnRowMapper.mapRow
        String sql = "select * from distx_app_node where app_name = ?";
        List<AppNode> taskList = jdbcTemplate.query(sql, new Object[]{appName}, appNodeMapper);
        return taskList;
    }

    /**
     * 获取有效数据
     *
     * @param jdbcTemplate
     * @param appName
     * @return
     */
    public List<AppNode> findAvaibleList(JdbcTemplate jdbcTemplate, String appName, Date avaibleTime) {
        //SingleColumnRowMapper.mapRow
        String sql = "select * from distx_app_node where app_name = ?  and status = 1 and last_heartbeat_time > ? order by id asc";
        List<AppNode> taskList = jdbcTemplate.query(sql, new Object[]{appName, avaibleTime}, appNodeMapper);
        return taskList;
    }


    public void insert(JdbcTemplate jdbcTemplate, AppNode appNode) {

        String sql = "INSERT INTO `distx_app_node` (`hash_code`,`ip`,`status`, `last_heartbeat_time`,`create_time`,`app_name`) " +
                "VALUES(?,?,?,?,?,?)";
        int result = jdbcTemplate.update(sql, appNode.getHashCode(), appNode.getIp(), appNode.getStatus(),
                appNode.getLastHeartBeatTime(), appNode.getCreateTime(), appNode.getAppName());

        Integer id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID() as value", Integer.class);
        appNode.setId(id);

        if (result == 1) {
            System.out.println("save ok");
        }


    }

    static class AppNodeMapper implements RowMapper<AppNode> {

        public AppNode mapRow(ResultSet rs, int rowNum) throws SQLException {
            AppNode appNode = new AppNode();
            appNode.setId(rs.getInt("id"));
            appNode.setAppName(rs.getString("app_name"));
            appNode.setHashCode(rs.getLong("hash_code"));
            appNode.setStatus(rs.getInt("status"));
            appNode.setIp(rs.getString("ip"));
            appNode.setLastHeartBeatTime(rs.getDate("last_heartbeat_time"));
            appNode.setCreateTime(rs.getDate("create_time"));
            return appNode;
        }

    }

}
