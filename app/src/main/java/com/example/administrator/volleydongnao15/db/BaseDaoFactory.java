package com.example.administrator.volleydongnao15.db;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

/**
 * Created by Administrator on 2017/1/9 0009.
 */

public class BaseDaoFactory {
    private String sqliteDatabasePath;

    private SQLiteDatabase sqLiteDatabase;

    private static  BaseDaoFactory instance=new BaseDaoFactory();
    private BaseDaoFactory()
    {
        sqliteDatabasePath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/teacher.db";
        openDatabase();
    }
    private void openDatabase() {
        this.sqLiteDatabase=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(sqliteDatabasePath,null);
    }
    public  synchronized  <T extends  BaseDao<M>,M> T getDao(Class<T> daoClazz, Class<M> entityClass)
    {
        BaseDao baseDao=null;
        try {
            baseDao=daoClazz.newInstance();
            baseDao.createTable(entityClass,sqLiteDatabase);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return (T) baseDao;
    }


    public  static  BaseDaoFactory getInstance()
    {
        return instance;
    }
}
