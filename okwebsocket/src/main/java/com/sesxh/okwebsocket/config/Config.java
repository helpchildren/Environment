package com.sesxh.okwebsocket.config;

import android.content.Context;

import com.sesxh.okwebsocket.ssl.SSLManager;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.OkHttpClient;

/**
 * @author LYH
 * @date 2021/1/27
 * @time 9:34
 * @desc 全局配置类
 **/
public class Config {

    private Context context;
    private OkHttpClient client;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier trustManager;
    private TimeUnit reconnectIntervalTimeUnit;
    private long reconnectInterval;
    private TimeUnit pingIntervalTimeUnit;
    private long pingInterval;

    private long timeoutInterval;
    private boolean debug;
    private boolean isTrustAll;


    public Config(Builder builder) {
        this.context=builder.context;
        this.client=builder.client;
        this.sslSocketFactory=builder.sslSocketFactory;
        this.trustManager=builder.trustManager;
        this.reconnectIntervalTimeUnit=builder.reconnectIntervalTimeUnit;
        this.reconnectInterval=builder.reconnectInterval;
        this.pingInterval=builder.pingInterval;
        this.pingIntervalTimeUnit=builder.pingIntervalTimeUnit;
        this.timeoutInterval=builder.timeoutInterval;
        this.debug=builder.debug;
        this.isTrustAll=builder.isTrustAll;
    }

    public static class Builder{
        private Context context;
        private OkHttpClient client;
        private SSLSocketFactory sslSocketFactory;
        private HostnameVerifier trustManager;
        private long reconnectInterval=2;//重连间隔时间
        private TimeUnit reconnectIntervalTimeUnit=TimeUnit.SECONDS;// 重连间隔时间单位
        private long pingInterval=0;//心跳间隔时间
        private TimeUnit pingIntervalTimeUnit=TimeUnit.SECONDS;// 心跳间隔时间单位
        private long timeoutInterval=10;//超时时间
        private boolean debug=false;// 是否是开发模式
        private boolean isTrustAll=true;//是否信任所有证书


        public Builder(Context context) {
            this.context = context;
        }

        public Builder client(OkHttpClient client) {
            if(client!=null) {
                this.client = client;
            }
            return this;
        }

        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public Builder trustManager(HostnameVerifier trustManager) {
            this.trustManager = trustManager;
            return this;
        }


        public Builder reconnectInterval(long reconnectInterval,TimeUnit reconnectIntervalTimeUnit) {
            if(reconnectInterval!=0) {
                this.reconnectInterval = reconnectInterval;
            }
            if(reconnectIntervalTimeUnit!=null) {
                this.reconnectIntervalTimeUnit = reconnectIntervalTimeUnit;
            }
            return this;
        }

        public Builder pingInterval(long pingInterval,TimeUnit pingIntervalTimeUnit) {
            if(pingInterval!=0) {
                this.pingInterval = pingInterval;
            }
            if(pingIntervalTimeUnit!=null) {
                this.pingIntervalTimeUnit = pingIntervalTimeUnit;
            }
            return this;
        }

        public void timeout(long timeout) {
            this.timeoutInterval=timeout;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder trustAll(boolean trustAll) {
            isTrustAll = trustAll;
            return this;
        }

        public Builder hosts(String... hosts) {
            // 默认添加基础域名
            if (this.trustManager == null) {
                this.trustManager = new SSLManager.SafeHostnameVerifier(hosts);
            } else {
                if (this.trustManager instanceof SSLManager.SafeHostnameVerifier) {
                    ((SSLManager.SafeHostnameVerifier) this.trustManager).addHosts(Arrays.asList(hosts));
                } else {
                    throw new IllegalArgumentException("please verifier host in your custom hostnameVerifier");
                }
            }
            return this;
        }

        public Config build(){
            return new Config(this);
        }
    }

    public Context getContext() {
        return context;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public HostnameVerifier getTrustManager() {
        return trustManager;
    }

    public TimeUnit getReconnectIntervalTimeUnit() {
        return reconnectIntervalTimeUnit;
    }

    public long getReconnectInterval() {
        return reconnectInterval;
    }

    public TimeUnit getPingIntervalTimeUnit() {
        return pingIntervalTimeUnit;
    }

    public long getPingInterval() {
        return pingInterval;
    }

    public long getTimeoutInterval() {
        return timeoutInterval;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isTrustAll() {
        return isTrustAll;
    }


}
