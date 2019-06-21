package com.jianliu.distributetx.config;

import com.jianliu.distributetx.util.CommonUtils;
import com.jianliu.distributetx.util.Hashing;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

/**
 * class GlobalTxConfig
 *
 * @author jianliu
 * @since 2019/6/19
 */
public class GlobalTxConfig implements InitializingBean {

    @Value("${distribute.tx.appName:}")
    private String appName;
    private String ip;
    /**
     * 当前实例的hash
     */
    private long hashCode;

    @Override
    public void afterPropertiesSet() throws Exception {
        if(StringUtils.isEmpty(appName)){
            throw new RuntimeException("distribute_tx的appName不能为空，请配置distribute.tx.appName=YourAppName");
        }
        ip = CommonUtils.getLocalIp();
        if (ip.equals("0")) {
            throw new RuntimeException("获取ip地址失败");
        }

        String rule = appName + "Node" + ip + "#1";
        hashCode = Hashing.MURMUR_HASH.hash(rule);
    }

    public String getAppName() {
        return appName;
    }

    public String getIp() {
        return ip;
    }

    public long getHashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "GlobalTxConfig{" +
                "appName='" + appName + '\'' +
                ", ip='" + ip + '\'' +
                ", hashCode=" + hashCode +
                '}';
    }
}
