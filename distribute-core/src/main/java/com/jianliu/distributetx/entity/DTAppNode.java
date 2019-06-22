package com.jianliu.distributetx.entity;


import java.util.Date;

/**
 * distributetx
 *
 * @author jian.liu
 * @since 2019/6/18
 */
public class DTAppNode {

    public final static Integer STATUS_ONLINE = 1;
    public final static Integer STATUS_OFFLINE = 0;

    private Integer id;

    /**
     * 应用名称
     */
    private String appName;
    /**
     * 用于一致性hash查询
     */
    private Long hashCode;
    /**
     * ip地址
     */
    private String ip;
    /**
     * 由心跳线程持续刷新时间，更新状态
     */
    private Date lastHeartBeatTime;
    /**
     * 1：在线，0：不在线
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Long getHashCode() {
        return hashCode;
    }

    public void setHashCode(Long hashCode) {
        this.hashCode = hashCode;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Date getLastHeartBeatTime() {
        return lastHeartBeatTime;
    }

    public void setLastHeartBeatTime(Date lastHeartBeatTime) {
        this.lastHeartBeatTime = lastHeartBeatTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
