package com.jianliu.distributetx.repository;

import com.jianliu.distributetx.BaseLogger;
import com.jianliu.distributetx.entity.DTAppNode;
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

    public void heartBeat(JdbcTemplate jdbcTemplate, DTAppNode appNode) {
        String sql = "update distx_app_node set status = ?,last_heartbeat_time=?,hash_code = ? where id = ?";
        jdbcTemplate.update(sql, appNode.getStatus(), appNode.getLastHeartBeatTime(), appNode.getHashCode(), appNode.getId());
    }

    public DTAppNode findByIp(JdbcTemplate jdbcTemplate, String appName, String ip) {
        String sql = "select * from distx_app_node where app_name =? and ip=? limit 1";
        List<DTAppNode> appNodeList = jdbcTemplate.query(sql, new Object[]{appName, ip}, appNodeMapper);
        if (!CollectionUtils.isEmpty(appNodeList)) {
            return appNodeList.get(0);
        }
        return null;
    }

    public List<DTAppNode> findList(JdbcTemplate jdbcTemplate, String appName) {
        //SingleColumnRowMapper.mapRow
        String sql = "select * from distx_app_node where app_name = ?";
        List<DTAppNode> taskList = jdbcTemplate.query(sql, new Object[]{appName}, appNodeMapper);
        return taskList;
    }

    /**
     * 获取有效数据
     *
     * @param jdbcTemplate
     * @param appName
     * @return
     */
    public List<DTAppNode> findAvaibleList(JdbcTemplate jdbcTemplate, String appName, Date avaibleTime) {
        //SingleColumnRowMapper.mapRow
        String sql = "select * from distx_app_node where app_name = ?  and status = 1 and last_heartbeat_time > ? order by id asc";
        List<DTAppNode> taskList = jdbcTemplate.query(sql, new Object[]{appName, avaibleTime}, appNodeMapper);
        return taskList;
    }


    public void insert(JdbcTemplate jdbcTemplate, DTAppNode appNode) {

        String sql = "INSERT INTO `distx_app_node` (`hash_code`,`ip`,`status`, `last_heartbeat_time`,`create_time`,`app_name`) " +
                "VALUES(?,?,?,?,?,?)";
        jdbcTemplate.update(sql, appNode.getHashCode(), appNode.getIp(), appNode.getStatus(),
                appNode.getLastHeartBeatTime(), appNode.getCreateTime(), appNode.getAppName());

        Integer id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID() as value", Integer.class);
        appNode.setId(id);
    }

    static class AppNodeMapper implements RowMapper<DTAppNode> {

        public DTAppNode mapRow(ResultSet rs, int rowNum) throws SQLException {
            DTAppNode appNode = new DTAppNode();
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
