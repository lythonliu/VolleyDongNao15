package com.example.administrator.volleydongnao15.http.download;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.administrator.volleydongnao15.db.BaseDaoFactory;
import com.example.administrator.volleydongnao15.http.HttpTaskRunnable;
import com.example.administrator.volleydongnao15.http.RequestInfo;
import com.example.administrator.volleydongnao15.http.download.dao.DownLoadRecordDao;
import com.example.administrator.volleydongnao15.http.download.enums.DownloadStatus;
import com.example.administrator.volleydongnao15.http.download.enums.DownloadStopMode;
import com.example.administrator.volleydongnao15.http.download.enums.Priority;
import com.example.administrator.volleydongnao15.http.download.interfaces.IDownloadCallback;
import com.example.administrator.volleydongnao15.http.download.interfaces.IDownloadServiceCallback;
import com.example.administrator.volleydongnao15.http.interfaces.IHttpListener;
import com.example.administrator.volleydongnao15.http.interfaces.IHttpService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Administrator on 2017/1/16 0016.
 */

public class FileDownManager implements IDownloadServiceCallback {
    private static final String TAG ="dongnao" ;
    //    private  static
    private byte[] lock=new byte[0];
    DownLoadRecordDao downLoadRecordDao = BaseDaoFactory.getInstance().getDao(DownLoadRecordDao.class,DownloadRecord.class);
    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    /**
     * 观察者模式
     */
    private final List<IDownloadCallback> downloadCallbacks = new CopyOnWriteArrayList<IDownloadCallback>();

    /**
     * 正在下载的所有任务
     */
    private static List<DownloadRecord> downloadingRecordList = new CopyOnWriteArrayList();

    Handler mainHandler =new Handler(Looper.getMainLooper());


    public int download(String url)
    {
        String[] split=url.split("/");
        return this.download(url,Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+split[split.length-1]);
    }
    public int download(String url, String filePath )
    {
        String[] split=url.split("/");
        String displayName=split[split.length-1];
        return this.download(url,filePath,displayName);
    }
    public int download(String url, String filePath, String displayName)
    {
        return this.download(url,filePath,displayName,Priority.middle);
    }

    public int download(String url, String targetFilePath,
                        String displayName , Priority priority ) {

        if(priority==null)
        {
            priority=Priority.low;
        }
        File targetFile=new File(targetFilePath);
        DownloadRecord downloadRecord = downLoadRecordDao.findRecord(url,targetFilePath);
        //根据URL和文件路径精确查找失败，改为文件路径查找
        if(downloadRecord ==null)
        {
            /**
             * 根据文件路径查找
             */
            List<DownloadRecord> sameDownloadRecords= downLoadRecordDao.findRecord(targetFilePath);
            /**
             * 大于0  表示下载
             */
            if(sameDownloadRecords.size()>0)
            {
                DownloadRecord downloadRecord1 =sameDownloadRecords.get(0);
                if(downloadRecord1.getCurrentLen()== downloadRecord1.getTotalLen())
                {
                    synchronized (downloadCallbacks)
                    {
                        for (IDownloadCallback downloadCallable: downloadCallbacks)
                        {
                            downloadCallable.onDownloadError(downloadRecord1.getId(),2,"文件已经下载了");
                        }
                    }

                }
            }
            /**
             * 插入数据库,创建一条记录
             */
            downloadRecord = downLoadRecordDao.addDownloadRecord(url,targetFilePath,displayName,priority.getValue());
            if(downloadRecord !=null)/*添加成功*/
            {
                synchronized (downloadCallbacks)
                {
                    for (IDownloadCallback downloadCallback: downloadCallbacks)
                    {
                        //通知应用层  数据库被添加了
                        downloadCallback.onDownloadRecordAdded(downloadRecord.getId());
                    }
                }
            }
            downloadRecord = downLoadRecordDao.findRecord(url,targetFilePath);
            if(this.isDowning(targetFile.getAbsolutePath()))
            {
                synchronized (downloadCallbacks)
                {
                    for (IDownloadCallback downloadCallable: downloadCallbacks)
                    {
                        downloadCallable.onDownloadError(downloadRecord.getId(),4,"正在下载，请不要重复添加");
                    }
                }
                return downloadRecord.getId();
            }

            if(downloadRecord !=null)
            {
               downloadRecord.setPriority(priority.getValue());
                //判断数据库存的 状态是否是完成
                if(downloadRecord.getStatus()!= DownloadStatus.FINISH.getValue())
                {
                    if(downloadRecord.getTotalLen()==0L||targetFile.length()==0L)
                    {
                        Log.i(TAG,"还未开始下载");
                        downloadRecord.setStatus(DownloadStatus.failed.getValue());
                    }
                    //判断数据库中 总长度是否等于文件长度
                    if(downloadRecord.getTotalLen()==targetFile.length()&& downloadRecord.getTotalLen()!=0)
                    {
                        downloadRecord.setStatus(DownloadStatus.FINISH.getValue());
                        synchronized (downloadCallbacks)
                        {
                            for (IDownloadCallback downloadCallback: downloadCallbacks)
                            {
                                try {
                                    downloadCallback.onDownloadError(downloadRecord.getId(),4,"已经下载了");
                                }catch (Exception e)
                                {
                                }
                            }
                        }
                    }
                }
                /**
                 * 更新
                 */
                downLoadRecordDao.updateRecord(downloadRecord);
            }

            /**
             * 判断是否已经下载完成
             */
            if(downloadRecord.getStatus()==DownloadStatus.FINISH.getValue())
            {
                Log.i(TAG,"已经下载完成  回调应用层");
                final int downId= downloadRecord.getId();
                synchronized (downloadCallbacks)
                {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (IDownloadCallback downloadCallback: downloadCallbacks)
                            {
                                downloadCallback.onDownloadStatusChanged(downId,DownloadStatus.FINISH);
                            }
                        }
                    });
                }
                downLoadRecordDao.removeRecordFromList(downId);
                return downloadRecord.getId();
            }//之前的下载 状态为暂停状态
            List<DownloadRecord> downloadingRecordList= FileDownManager.downloadingRecordList;
            //当前下载不是最高级  则先退出下载
            if(priority!=Priority.high)
            {
                for(DownloadRecord downloadingRecord:downloadingRecordList)
                {
                    //从下载表中  获取到全部正在下载的任务
                    downloadingRecord= downLoadRecordDao.findSingleRecord(downloadingRecord.getFilePath());

                    if(downloadRecord !=null&& downloadRecord.getPriority()==Priority.high.getValue())
                    {
                        if(downloadRecord.getFilePath().equals(downloadingRecord.getFilePath()))
                        {
                            return downloadRecord.getId();
                        }
                    }
                }
            }
            doDownLoad(downloadRecord);
            if(priority==Priority.high||priority== Priority.middle)
            {
                synchronized (downloadingRecordList)
                {
                    for (DownloadRecord downloadRecord1 :downloadingRecordList)
                    {
                        if(!downloadRecord.getFilePath().equals(downloadRecord1.getFilePath()))
                        {
                            DownloadRecord downingInfo= downLoadRecordDao.findSingleRecord(downloadRecord1.getFilePath());
                            if(downingInfo!=null)
                            {
                                pause(downloadRecord.getId(),DownloadStopMode.auto);
                            }
                        }
                    }
                }
                return downloadRecord.getId();
            }
            return -1;
        }


      return  -1;
    }


    /**
     * 停止
     * @param downloadId
     * @param downloadStopMode
     */
    public void pause(int downloadId, DownloadStopMode downloadStopMode)
    {
        if (downloadStopMode == null)
        {
            downloadStopMode = DownloadStopMode.auto;
        }
        final DownloadRecord downloadRecord = downLoadRecordDao.findRecordById(downloadId);
        if (downloadRecord != null)
        {
            // 更新停止状态
            if (downloadRecord != null)
            {
                downloadRecord.setStopMode(downloadStopMode.getValue());
                downloadRecord.setStatus(DownloadStatus.pause.getValue());
                downLoadRecordDao.updateRecord(downloadRecord);
            }
            for (DownloadRecord downloadingRecord: downloadingRecordList)
            {
                if(downloadId==downloadingRecord.getId())
                {
                    downloadingRecord.getHttpTaskRunnable().pause();
                }
            }
        }
    }

    /**
     * 判断当前是否正在下载
     *
     * @param absolutePath
     * @return
     */
    private boolean isDowning(String absolutePath) {
        for (DownloadRecord downloadRecord : downloadingRecordList)
        {
            if(downloadRecord.getFilePath().equals(absolutePath))
            {
                return true;
            }
        }
        return false;
    }



    /**
     * 添加观察者
     * @param downloadCallable
     */
    public void setDownCallable(IDownloadCallback downloadCallable)
    {
        synchronized (downloadCallbacks)
        {
             downloadCallbacks.add(downloadCallable);
        }

    }

        /**
         * 下载
         * @param url
         * MainAcitivity
         * 1
         *
         * 2
         *
         */
    /**
     * 下载
     */
    public DownloadRecord doDownLoad(DownloadRecord downloadRecord)
    {
        synchronized (lock)
        {
            RequestInfo requestInfo =new RequestInfo();
            IHttpService fileDownHttpService=new FileDownHttpService();
            Map<String,String> httpHeadMap=fileDownHttpService.getHttpHeadMap();
            /**
             * 处理结果的策略
             */
            IHttpListener httpListener=new DownLoadHttpListener(downloadRecord,this,fileDownHttpService);
            requestInfo.setHttpListener(httpListener);
            requestInfo.setHttpService(fileDownHttpService);
            requestInfo.setUrl(downloadRecord.getUrl());
            HttpTaskRunnable httpTaskRunnable =new HttpTaskRunnable(requestInfo);
            downloadRecord.setHttpTaskRunnable(httpTaskRunnable);
            downloadingRecordList.add(downloadRecord);
            httpTaskRunnable.start();

        }

        return downloadRecord;

    }






    @Override
    public void onDownloadStatusChanged(DownloadRecord downloadRecord) {

    }

    @Override
    public void onTotalLengthReceived(DownloadRecord downloadRecord) {

    }

    @Override
    public void onCurrentSizeChanged(DownloadRecord downloadRecord, double downLenth, long speed) {
        Log.i(TAG,"下载速度："+ speed/1000 +"k/s");
        Log.i(TAG,"-----路径  "+ downloadRecord.getFilePath()+"  下载长度  "+downLenth+"   速度  "+speed);
    }

    @Override
    public void onDownloadSuccess(DownloadRecord downloadRecord) {
        Log.i(TAG,"下载成功    路劲  "+ downloadRecord.getFilePath()+"  url "+ downloadRecord.getUrl());
    }

    @Override
    public void onDownloadPause(DownloadRecord downloadRecord) {

    }

    @Override
    public void onDownloadError(DownloadRecord downloadRecord, int var2, String var3) {

    }
}
