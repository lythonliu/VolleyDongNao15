package com.example.administrator.volleydongnao15.http.download.interfaces;


import com.example.administrator.volleydongnao15.http.download.DownloadRecord;

/**
 * Created by david on 2017/1/12.
 */
public interface IDownloadServiceCallback {
    void onDownloadStatusChanged(DownloadRecord downloadRecord);

    void onTotalLengthReceived(DownloadRecord downloadRecord);

    void onCurrentSizeChanged(DownloadRecord downloadRecord, double downLenth, long speed);

    void onDownloadSuccess(DownloadRecord downloadRecord);

    void onDownloadPause(DownloadRecord downloadRecord);

    void onDownloadError(DownloadRecord downloadRecord, int var2, String var3);
}
