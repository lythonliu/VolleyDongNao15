package com.example.administrator.volleydongnao15.http.interfaces;

/**
 * Created by Administrator on 2017/1/13 0013.
 */

import org.apache.http.HttpEntity;

import java.util.Map;

/**
 *获取网络
 */
public interface IHttpService {
    /**
     * 设置url
     * @param url
     */
    void setUrl(String url);

    /**
     * 执行获取网络
     */
    void execute();

    /**
     * 设置处理接口
     * @param httpListener
     */
    void setHttpListener(IHttpListener httpListener);

    /**
     * 设置请求参数
     * String  1
     * byte[]  2
     *
     */
    void setRequestData(byte[] requestData);

    void pause();

    /**
     *
     * 以下的方法是 额外添加的
     * 获取请求头的map
     * @return
     */
     Map<String,String> getHttpHeadMap();

     boolean cancle();

     boolean isCancle();

     boolean isPause();


}
