package com.example.administrator.volleydongnao15.http;

import com.example.administrator.volleydongnao15.http.interfaces.IRequestCallBack;
import com.example.administrator.volleydongnao15.http.interfaces.IHttpListener;
import com.example.administrator.volleydongnao15.http.interfaces.IHttpService;

import java.util.concurrent.FutureTask;

/**
 * Created by Administrator on 2017/1/13 0013.
 */

public class Volley {
    /**
     *
     * @param <T>  请求参数类型
     * @param <M>  响应参数类型
     *           暴露给调用层
     */
    public static <T,M> void sendRequest(T  requestInfo, String url,
                                         Class<M> response, IRequestCallBack dataListener)
    {
        RequestInfo<T> requestHodler=new RequestInfo<>();
        requestHodler.setUrl(url);
        IHttpService httpService=new JsonHttpService();
        IHttpListener httpListener=new JsonHttpListener<>(response,dataListener);
        requestHodler.setHttpService(httpService);
        requestHodler.setHttpListener(httpListener);
        //将请求参数赋值
        requestHodler.setRequestInfo(requestInfo);

        HttpTaskRunnable<T> httpTaskRunnable =new HttpTaskRunnable<>(requestHodler);
        try {
            ThreadPoolManager.getInstance().execte(new FutureTask<Object>(httpTaskRunnable,null));
        } catch (InterruptedException e) {
            dataListener.onErro();
        }
    }

}
