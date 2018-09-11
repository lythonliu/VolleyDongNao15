package com.example.administrator.volleydongnao15.http.interfaces;

/**
 * Created by Administrator on 2017/1/13 0013.
 */

public interface IRequestCallBack<M> {
    /**
     * 回调结果给调用层
     * @param m
     */
     void onSuccess(M m);


      void onErro();
}
