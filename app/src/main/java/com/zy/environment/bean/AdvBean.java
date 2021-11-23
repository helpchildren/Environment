package com.zy.environment.bean;

/*
* 广告类
* */
public class AdvBean {

    private String id;//广告id
    private String screen_name;//广告名
    private String type;//广告类型 1：图片 2：视频
    private String url;//下载路径

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScreen_name() {
        return screen_name;
    }

    public void setScreen_name(String screen_name) {
        this.screen_name = screen_name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
