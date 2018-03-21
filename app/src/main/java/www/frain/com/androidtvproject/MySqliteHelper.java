package www.frain.com.androidtvproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by diy on 2018/3/20.
 */

public class MySqliteHelper extends SQLiteOpenHelper {
    private static final String db_name = "mydata.db"; // 数据库名称
    String stu_table = "create table recordTable(_id integer primary key autoincrement,url text,time text)";
    //数据库版本号
    private static Integer Version = 1;
    public MySqliteHelper(Context context)
    {
        this(context, db_name, Version);
    }


    public MySqliteHelper(Context context,String name)
    {
        this(context, name, Version);
    }

    public MySqliteHelper(Context context,String name,int version)
    {
        this(context,name,null,version);
    }

    public MySqliteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public MySqliteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(stu_table);
    }

    public void insert(SQLiteDatabase db, String url, String time) {
        //实例化常量值
        ContentValues cValue = new ContentValues();
        //添加用户名
        cValue.put("url", url);
        //添加密码
        cValue.put("time", time);
        //调用insert()方法插入数据
        db.insert("recordTable", null, cValue);
    }

    public void update(SQLiteDatabase db, String url, String time) {
        //实例化内容值
        ContentValues values = new ContentValues();
        //在values中添加内容
        values.put("time", time);
        //修改条件
        String whereClause = "url=?";
        //修改添加参数
        String[] whereArgs = {url};
        //修改
        db.update("recordTable", values, whereClause, whereArgs);
    }

    public boolean query(SQLiteDatabase db, String url) {
        //查询获得游标
        Cursor cursor = db.query("recordTable", null, "url=?", new String[]{url}, null, null, null);
        //判断游标是否为空
        if (cursor.moveToFirst()) {
            return true;
        }else{
            return false;
        }
    }
    public long queryTime(SQLiteDatabase db, String url) {
        //查询获得游标
        Cursor cursor = db.query("recordTable", null, "url=?", new String[]{url}, null, null, null);
        //判断游标是否为空
        if (cursor.moveToFirst()) {
            return Long.parseLong(cursor.getString(cursor.getColumnIndex("time")));
        }else{
            return 0;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
