package com.jianliu.distributetx.util;

import java.net.InetAddress;

/**
 * class CommonUtils
 *
 * @author jianliu
 * @since 2019/6/19
 */
public class CommonUtils {

    /**
     * 获取本机ip
     *
     * @return
     */
    public static String getLocalIp() {
        return new DefaultIPHolder().getLocalIp();
    }


    public interface IPHolder {
        String getLocalIp();
    }

    public static class DefaultIPHolder implements IPHolder {
        @Override
        public String getLocalIp() {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                return addr.getHostAddress();
            } catch (Exception e) {
                return "0";
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(CommonUtils.getLocalIp());
    }

}
