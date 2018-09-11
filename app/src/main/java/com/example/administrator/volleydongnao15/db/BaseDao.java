package com.example.administrator.volleydongnao15.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;


import com.example.administrator.volleydongnao15.db.annotion.DbFiled;
import com.example.administrator.volleydongnao15.db.annotion.DbTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 2017/1/12.
 */

public abstract class BaseDao<T> implements IBaseDao<T> {
    private boolean isInit = false;
    protected SQLiteDatabase sqLiteDatabase;
    private Class<T> entityClass;
    private String tableName;
    private Map<String, Field> columnFiledMap;

    public String getTableName() {
        return tableName;
    }

    @Override
    public Long insert(T entity) {
        ContentValues contentValues = getContentValues(entity);
        long result = sqLiteDatabase.insert(tableName, null, contentValues);
        return result;
    }

    private ContentValues getContentValues(T entity) {
        ContentValues contentValues = new ContentValues();
        try {
            for (Map.Entry<String,Field> entry: columnFiledMap.entrySet()){
                /*获取entity的Field的值*/
                if(entry.getValue().get(entity)==null){
                    continue;
                }
                contentValues.put(entry.getKey(),entry.getValue().get(entity).toString());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return contentValues;
    }

    @Override
    public int update(T entity, T where) {
        ContentValues contentValues = getContentValues(entity);
        Condition condition = new Condition(getContentValues(where));
        int update = sqLiteDatabase.update(tableName, contentValues, condition.whereClause, condition.whereArgs);
        return update;
    }

    @Override
    public int delete(T entity) {
        Condition condition = new Condition(getContentValues(entity));
        int delete = sqLiteDatabase.delete(tableName, condition.whereClause, condition.whereArgs);
        return delete;
    }

    @Override
    public List<T> query(T entity) {
        return query(entity,null,null,null);
    }

    @Override
    public List<T> query(T entity, String orderBy, Integer startIndex, Integer limit) {
        String limitString = null;
        if(startIndex!=null && limit!=null){
            limitString = startIndex+","+limit;
        }
        Condition condition = new Condition(getContentValues(entity));
        Cursor cursor = null;
        List<T> result =new ArrayList<>();
        try
        {
            cursor = sqLiteDatabase.query(tableName, null,condition.getWhereClause(),condition.whereArgs,null,null,orderBy,limitString);
            result= convertResult(cursor,entity);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(cursor!=null){
                cursor.close();
            }
        }
        return result;
    }

    protected List<T> convertResult(Cursor cursor, T entity) {
        ArrayList list = new ArrayList();
        Object entityObject;
        while(cursor.moveToNext()){
            try {
                entityObject = entity.getClass().newInstance();
                Iterator<Map.Entry<String, Field>> iterator = columnFiledMap.entrySet().iterator();
                while(iterator.hasNext()){
                    Map.Entry<String, Field> entry = iterator.next();
                    String columnName = entry.getKey();
                    Field field = entry.getValue();
                    int columnIndex = cursor.getColumnIndex(columnName);
                    Class type = field.getType();
                    if(columnIndex!=-1){
                        if(type==String.class){
                            field.set(entityObject,cursor.getString(columnIndex));
                        }else if(type==Double.class){
                            field.set(entityObject,cursor.getDouble(columnIndex));
                        }else if(type== Integer.class){
                            int value =cursor.getInt(columnIndex);
                            Log.i("dongnao","value="+value);
                            field.set(entityObject,cursor.getInt(columnIndex));
                        }else if(type == Long.class){
                            field.set(entityObject,cursor.getLong(columnIndex));
                        }else if(type == byte[].class){
                            field.set(entityObject,cursor.getBlob(columnIndex));
                        }else{
                            /**
                             * 不支持的类型
                             */
                            continue;
                        }
                    }
                }
                list.add(entityObject);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    protected synchronized boolean createTable(Class<T> entity, SQLiteDatabase sqLiteDatabase) {
        if (!isInit) {
            this.sqLiteDatabase = sqLiteDatabase;
            this.entityClass = entity;
            if (entity.getAnnotation(DbTable.class) == null) {
                tableName = entity.getClass().getSimpleName();
            } else {
                tableName = entity.getAnnotation(DbTable.class).value();
            }
            if (!sqLiteDatabase.isOpen()) {
                return false;
            }

            if (!TextUtils.isEmpty(createTableSQL())) {
                sqLiteDatabase.execSQL(createTableSQL());
            }
            columnFiledMap = new HashMap<>();
            columnFiledMap();
            isInit = true;
        }
        return isInit;
    }

    /**
     * 维护映射关系
     */
    private void columnFiledMap() {
        String sql = "select * from " + this.tableName + " limit 1,0";
        Cursor cursor = null;
        try {
            cursor = sqLiteDatabase.rawQuery(sql, null);
            /**
             * 表的列名数组
             */
            String[] cursorColumnNames = cursor.getColumnNames();
            /**
             * 拿到Filed数组
             */
            Field[] entityClassFields = entityClass.getFields();
            for (Field entityClassField : entityClassFields) {
                entityClassField.setAccessible(true);

                Field field = null;
                String columnName = null;
                /**
                 * 开始找对应关系
                 */
                for (String cursorColumnName : cursorColumnNames) {
                    String filedAnnotationName = null;
                    if(entityClassField.getAnnotation(DbFiled.class)!=null){
                        filedAnnotationName = entityClassField.getAnnotation(DbFiled.class).value();
                    }else {
                        filedAnnotationName = entityClassField.getName();
                    }
                    if(cursorColumnName.equals(filedAnnotationName)){
                        field = entityClassField;
                        columnName = cursorColumnName;
                        break;
                    }
                }
                //找到了对应关系
                if(field!=null){
                    columnFiledMap.put(columnName,field);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }


    public abstract String createTableSQL();




    class Condition{
        /**
         * 查询条件
         * name=? && password =?
         */
        private String whereClause;

        private  String[] whereArgs;
        public Condition(ContentValues whereClause) {
            ArrayList args = new ArrayList();
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(" 1=1 ");

            Set keys=whereClause.keySet();
            Iterator iterator=keys.iterator();
            while (iterator.hasNext())
            {
                String key= (String) iterator.next();
                String value= (String) whereClause.get(key);

                if (value!=null)
                {
                    /*
                    拼接条件查询语句
                    1=1 and name =? and password=?
                     */
                    stringBuilder.append(" and "+key+" =?");
                    /**
                     * ？----》value
                     */
                    args.add(value);
                }
            }
            this.whereClause=stringBuilder.toString();
            this.whereArgs= (String[]) args.toArray(new String[args.size()]);

        }

        public String[] getWhereArgs() {
            return whereArgs;
        }

        public String getWhereClause() {
            return whereClause;
        }
    }

}
