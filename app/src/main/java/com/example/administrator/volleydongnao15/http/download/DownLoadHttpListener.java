package com.example.administrator.volleydongnao15.http.download;

import android.os.Handler;
import android.os.Looper;

import com.example.administrator.volleydongnao15.http.download.enums.DownloadStatus;
import com.example.administrator.volleydongnao15.http.download.interfaces.IDownLitener;
import com.example.administrator.volleydongnao15.http.download.interfaces.IDownloadServiceCallback;
import com.example.administrator.volleydongnao15.http.interfaces.IHttpService;

import org.apache.http.HttpEntity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by Administrator on 2017/1/16 0016.
 * 1
 * DownItenInfo
 */

public class DownLoadHttpListener implements IDownLitener{

    private DownloadRecord downloadRecord;

    private File targetFile;
    protected  String url;
    private long breakPoint;
    private IDownloadServiceCallback downloadServiceCallback;

    private IHttpService httpService;
    /**
     * 得到主线程
     */
    private Handler handler=new Handler(Looper.getMainLooper());
    public DownLoadHttpListener(DownloadRecord downloadRecord,
                                IDownloadServiceCallback downloadServiceCallback,
                                IHttpService httpService) {
        this.downloadRecord = downloadRecord;
        this.downloadServiceCallback = downloadServiceCallback;
        this.httpService = httpService;
        this.targetFile =new File(downloadRecord.getFilePath());
        /**
         * 得到已经下载的长度
         */
        this.breakPoint= targetFile.length();
    }

    /**
     * 2
     * @param headerMap
     */
    public void addHttpHeader(Map<String,String> headerMap)
    {
        long length= getTargetFile().length();
        if(length>0L)
        {
            headerMap.put("RANGE","bytes="+length+"-");
        }

    }
    public DownLoadHttpListener(DownloadRecord downloadRecord) {
        this.downloadRecord = downloadRecord;
    }

    @Override
    public void setHttpServive(IHttpService httpServive) {
        this.httpService=httpServive;
    }

    /**
     * 设置取消接口
     */
    @Override
    public void setCancleCalle() {

    }

    @Override
    public void setPuaseCallble() {

    }

    @Override
    public void onSuccess(HttpEntity httpEntity) {
        InputStream inputStream = null;
        try {
            inputStream = httpEntity.getContent();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long contentLength = httpEntity.getContentLength();
        long totalLength = this.breakPoint + contentLength;
        this.updateTotalLength(totalLength);
        this.downloadStatusChange(DownloadStatus.downloading);

        long startTime = System.currentTimeMillis();
        //用于计算每秒多少k
        long speed = 0L;
        //花费时间
        long useTime = 0L;
        //下载的长度
        long gotLength = 0L;
        //接受的长度
        long receiveLen = 0L;
        boolean bufferLen = false;
        //单位时间下载的字节数
        long calcSpeedLen = 0L;
        //总数

        byte[] buffer = new byte[512];
        int count = 0;
        long currentTime = System.currentTimeMillis();
        BufferedOutputStream bufferedOutputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            if (!makeDir(this.getTargetFile().getParentFile())) {
                downloadServiceCallback.onDownloadError(downloadRecord,1,"创建文件夹失败");
            } else {
                fileOutputStream = new FileOutputStream(this.getTargetFile(), true);
                bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    if (this.getHttpService().isCancle()) {
                        downloadServiceCallback.onDownloadError(downloadRecord, 1, "用户取消了");
                        return;
                    }

                    if (this.getHttpService().isPause()) {
                        downloadServiceCallback.onDownloadError(downloadRecord, 2, "用户暂停了");
                        return;
                    }
                    bufferedOutputStream.write(buffer, 0, length);
                    gotLength += (long) length;
                    receiveLen += (long) length;
                    calcSpeedLen += (long) length;
                    ++count;
                    if (receiveLen * 10L / totalLength >= 1L || count >= 5000) {
                        currentTime = System.currentTimeMillis();
                        useTime = currentTime - startTime;
                        startTime = currentTime;
                        speed = 1000L * calcSpeedLen / useTime;
                        count = 0;
                        calcSpeedLen = 0L;
                        receiveLen = 0L;
                        //应该保存数据库
                        this.downloadLengthChange(this.breakPoint + gotLength, totalLength, speed);
                    }
                }
                bufferedOutputStream.close();
                inputStream.close();
                if (contentLength != gotLength) {
                    downloadServiceCallback.onDownloadError(downloadRecord, 3, "下载长度不相等");
                } else {
                    this.downloadLengthChange(this.breakPoint + gotLength, totalLength, speed);
                    this.downloadServiceCallback.onDownloadSuccess(downloadRecord.copy());
                }
            }
        } catch (IOException ioException) {
            if (this.getHttpService() != null) {
//                this.getHttpService().abortRequest();
            }
            return;
        } catch (Exception e) {
            if (this.getHttpService() != null) {
//                this.getHttpService().abortRequest();
            }
        } finally {
            try {
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }

                if (httpEntity != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * 创建文件夹的操作
     * @param parentFile
     * @return
     */
    private boolean makeDir(File parentFile) {
        return parentFile.exists()&&!parentFile.isFile()
                ?parentFile.exists()&&parentFile.isDirectory():
                parentFile.mkdirs();
    }


    private void downloadLengthChange(final long downlength, final long totalLength, final long speed) {

        downloadRecord.setCurrentTotalLength(downlength);
        if(downloadServiceCallback !=null)
        {
            DownloadRecord copyDownItenIfo= downloadRecord.copy();
            synchronized (this.downloadServiceCallback)
            {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        downloadServiceCallback.onCurrentSizeChanged(downloadRecord,downlength/totalLength,speed);
                    }
                });
            }

        }

    }

    /**
     * 更改下载时的状态
     * @param downloading
     */
    private void downloadStatusChange(DownloadStatus downloading) {
        downloadRecord.setStatus(downloading.getValue());
        final DownloadRecord copyDownloadRecord = downloadRecord.copy();
        if(downloadServiceCallback !=null)
        {
            synchronized (this.downloadServiceCallback)
            {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        downloadServiceCallback.onDownloadStatusChanged(copyDownloadRecord);
                    }
                });
            }
        }
    }

    /**
     * 回调  长度的变化
     * @param totalLength
     */
    private void updateTotalLength(long totalLength) {
        downloadRecord.setCurrentTotalLength(totalLength);
        final DownloadRecord copyDownloadRecord = downloadRecord.copy();
        if(downloadServiceCallback !=null)
        {
            synchronized (this.downloadServiceCallback)
            {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        downloadServiceCallback.onTotalLengthReceived(copyDownloadRecord);
                    }
                });
            }
        }

    }

    @Override
    public void onFail() {

    }

    public IHttpService getHttpService() {
        return httpService;
    }

    public File getTargetFile() {
        return targetFile;
    }
}
