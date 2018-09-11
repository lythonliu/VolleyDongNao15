package com.example.administrator.volleydongnao15.http.download.dao;

import android.database.Cursor;

import com.example.administrator.volleydongnao15.db.BaseDao;
import com.example.administrator.volleydongnao15.http.download.DownloadRecord;
import com.example.administrator.volleydongnao15.http.download.enums.DownloadStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2017/1/18 0018.
 */

public class DownLoadRecordDao extends BaseDao<DownloadRecord> {
    /**
     * 保存应该下载的集合
     * 不包括已经下载成功的
     */
    private List<DownloadRecord> downloadRecordList =
            Collections.synchronizedList(new ArrayList<DownloadRecord>());

    private DownloadInfoComparator downloadInfoComparator=new DownloadInfoComparator();

    @Override
    public String createTableSQL() {
        return new StringBuilder()
                .append("create table if not exists  t_DownloadRecord(")
                .append("id Integer primary key, ")
                .append("url TEXT not null,")
                .append("filePath TEXT not null, ")
                .append("displayName TEXT, ")
                .append("status Integer, ")
                .append("totalLen Long, ")
                .append("currentLen Long,")
                .append("startTime TEXT,")
                .append("finishTime TEXT,")
                .append("userId TEXT, ")
                .append("httpTaskType TEXT,")
                .append("priority  Integer,")
                .append("stopMode Integer,")
                .append("downloadMaxSizeKey TEXT,")
                .append("unique(filePath))")
                .toString();
    }

    @Override
    public List<DownloadRecord> query(String sql) {
        return null;
    }
    /**
     * id
     */
    /**
     * 生成下载id
     *
     * @return 返回下载id
     */
    private Integer queryRecordId()
    {
        int maxId = 0;
        String sql = "select max(id)  from " +getTableName() ;
        synchronized (DownLoadRecordDao.class)
        {
            Cursor cursor = this.sqLiteDatabase.rawQuery(sql,null);
            if(cursor.moveToNext())
            {
                String[] colmName=cursor.getColumnNames();

                int index=cursor.getColumnIndex("max(id)");
                if(index!=-1)
                {
                    Object value =cursor.getInt(index);
                    if (value != null)
                    {
                        maxId = Integer.parseInt(String.valueOf(value));
                    }
                }
            }

        }
        return maxId + 1;
    }

    /**
     * 根据下载地址和下载文件路径查找下载记录
     *
     * @param url
     *            下载地址
     * @param filePath
     *            下载文件路径
     * @return
     */
    public DownloadRecord findRecord(String url, String filePath)
    {
        synchronized (DownLoadRecordDao.class)
        {
            /*缓存中查找记录*/
            for (DownloadRecord record : downloadRecordList)
            {
                if (record.getUrl().equals(url) && record.getFilePath().equals(filePath))
                {
                    return record;
                }
            }
            /**
             * 内存集合找不到
             * 就从数据库中查找
             */
            DownloadRecord downloadRecord = new DownloadRecord();
            downloadRecord.setUrl(url);
            downloadRecord.setFilePath(filePath);
            List<DownloadRecord> resultList = super.query(downloadRecord);
            if (resultList.size() > 0)
            {
                return resultList.get(0);
            }
            return null;
        }

    }

    /**
     * 根据 下载文件路径查找下载记录
     *
     *            下载地址
     * @param filePath
     *            下载文件路径
     * @return
     */
    public List<DownloadRecord> findRecord(String filePath)
    {
        synchronized (DownLoadRecordDao.class)
        {
            DownloadRecord where = new DownloadRecord();
            where.setFilePath(filePath);
            List<DownloadRecord> resultList = super.query(where);
            return resultList;
        }

    }

    /**
     * 添加下载记录
     *
     * @param url
     *            下载地址
     * @param filePath
     *            下载文件路径
     * @param displayName
     *            文件显示名
     * @param priority
     *            小组优先级
     *            TODO
     * @return 下载id
     */
    public DownloadRecord addDownloadRecord(String url, String filePath, String displayName , int priority)
    {
        synchronized (DownLoadRecordDao.class)
        {
            DownloadRecord existDownloadInfo = findRecord(url, filePath);
            if (existDownloadInfo == null)
            {
                DownloadRecord downloadRecord = new DownloadRecord();
                downloadRecord.setId(queryRecordId());
                downloadRecord.setUrl(url);
                downloadRecord.setFilePath(filePath);
                downloadRecord.setDisplayName(displayName);
                downloadRecord.setStatus(DownloadStatus.waitting.getValue());
                downloadRecord.setTotalLen(0L);
                downloadRecord.setCurrentLen(0L);
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                downloadRecord.setStartTime(dateFormat.format(new Date()));
                downloadRecord.setFinishTime("0");
                downloadRecord.setPriority(priority);
                super.insert(downloadRecord);
                downloadRecordList.add(downloadRecord);
                return downloadRecord;
            }
            return null;
        }
    }

    /**
     * 更新下载记录
     *
     * @param downloadRecord
     *            下载记录
     * @return
     */
    public int updateRecord(DownloadRecord downloadRecord)
    {
        DownloadRecord where = new DownloadRecord();
        where.setId(downloadRecord.getId());
        int result = 0;
        synchronized (DownLoadRecordDao.class)
        {
            try
            {
                result = super.update(downloadRecord, where);
            }catch (Throwable e)
            {
            }
            if (result > 0)
            {
                for (int i = 0; i < downloadRecordList.size(); i++)
                {
                    if (downloadRecordList.get(i).getId().intValue() == downloadRecord.getId())
                    {
                        downloadRecordList.set(i, downloadRecord);
                        break;
                    }
                }
            }
        }
        return result;
    }
    /**
     * 根据下载地址和下载文件路径查找下载记录
     *
     *            下载地址
     * @param filePath
     *            下载文件路径
     * @return
     */
    public DownloadRecord findSingleRecord(String filePath)
    {
        List<DownloadRecord> downloadInfoList = findRecord(filePath);
        if(downloadInfoList.isEmpty())
        {
            return null;
        }
        return downloadInfoList.get(0);
    }
    /**
     * 根据id查找下载记录对象
     *
     * @param recordId
     * @return
     */
    public DownloadRecord findRecordById(int recordId)
    {
        synchronized (DownLoadRecordDao.class)
        {
            for (DownloadRecord record : downloadRecordList)
            {
                if (record.getId() == recordId)
                {
                    return record;
                }
            }

            DownloadRecord where = new DownloadRecord();
            where.setId(recordId);
            List<DownloadRecord> resultList = super.query(where);
            if (resultList.size() > 0)
            {
                return resultList.get(0);
            }
            return null;
        }

    }
    /**
     * 根据id从内存中移除下载记录
     *
     * @param id
     *            下载id
     * @return true标示删除成功，否则false
     */
    public boolean removeRecordFromList(int id)
    {
        synchronized (DownloadRecord.class)
        {
            for (int i = 0; i < downloadRecordList.size(); i++)
            {
                if (downloadRecordList.get(i).getId() == id)
                {
                    downloadRecordList.remove(i);
                    break;
                }
            }
            return true;
        }
    }

    /**
     * 比较器
     */
    class DownloadInfoComparator implements Comparator<DownloadRecord>
    {
        @Override
        public int compare(DownloadRecord lhs, DownloadRecord rhs)
        {
            return rhs.getId() - lhs.getId();
        }
    }
}
