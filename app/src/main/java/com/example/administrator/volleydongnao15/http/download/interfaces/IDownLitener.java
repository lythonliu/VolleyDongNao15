package com.example.administrator.volleydongnao15.http.download.interfaces;

import com.example.administrator.volleydongnao15.http.interfaces.IHttpListener;
import com.example.administrator.volleydongnao15.http.interfaces.IHttpService;

/**
 * Created by Administrator on 2017/1/16 0016.
 */

public interface IDownLitener  extends IHttpListener{

    void setHttpServive(IHttpService  httpServive);


    void  setCancleCalle();


    void  setPuaseCallble();

}
