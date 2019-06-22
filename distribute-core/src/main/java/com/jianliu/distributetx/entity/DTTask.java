package com.jianliu.distributetx.entity;


import java.util.Date;

/**
 * distributetx
 *
 * @author jian.liu
 * @since 2019/6/18
 */
public class DTTask {

    public final static Integer STATUS_CREATE = 1;
    public final static Integer STATUS_DOING = 2;
    public final static Integer STATUS_SUCCESS = 3;
    public final static Integer STATUS_FAILURE = 4;
    //异常处理结束
    public final static Integer STATUS_HANDLED = 5;
    //任务已无效，如bean已不存在
    public final static Integer STATUS_INVALID_TASK = 6;

    private Integer id;

    /**
     * 应用名称
     */
    private String appName;
    /**
     * uuid 事务id
     */
    private String txId;
    /**
     * 任务详情，序列化为byte[]
     */
    private byte[] taskDetail;
    /**
     * 一致性hash查询
     */
    private Long hashCode;
    /**
     * 1:创建，2：消费中，3：成功，4：失败
     */
    private Integer status;
    /**
     * 重试次数
     */
    private Integer tryTimes;

    /**
     * 记录版本
     */
    private Integer version;

    /**
     * 消费时间
     */
    private Date consumerTime;
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

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public byte[] getTaskDetail() {
        return taskDetail;
    }

    public void setTaskDetail(byte[] taskDetail) {
        this.taskDetail = taskDetail;
    }

    public Long getHashCode() {
        return hashCode;
    }

    public void setHashCode(Long hashCode) {
        this.hashCode = hashCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getTryTimes() {
        return tryTimes;
    }

    public void setTryTimes(Integer tryTimes) {
        this.tryTimes = tryTimes;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Date getConsumerTime() {
        return consumerTime;
    }

    public void setConsumerTime(Date consumerTime) {
        this.consumerTime = consumerTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
