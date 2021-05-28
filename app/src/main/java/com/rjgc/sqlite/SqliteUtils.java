package com.rjgc.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SqliteUtils {

    private final SQLiteDatabase db;
    private final String remoteApiUrl;
    private final Timer timer = new Timer();
    private static volatile SqliteUtils instance;

    public static SqliteUtils getInstance(Context context, String remoteApiUrl) {
        if (instance == null) {
            synchronized (SqliteUtils.class) {
                if (instance == null) {
                    instance = new SqliteUtils(context, remoteApiUrl);
                }
            }
        }
        return instance;
    }

    private SqliteUtils(Context context, String remoteApiUrl) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context, "pest-identification.db", null, 1);
        this.db = databaseHelper.getWritableDatabase();
        this.remoteApiUrl = remoteApiUrl;
        SqliteUtils self = this;
        TimerTask syncTask = new TimerTask() {
            @Override
            public void run() {
                self.syncFromRemote();
            }
        };
        //每十分钟同步一次数据库
        this.timer.schedule(syncTask, 0, 1000 * 60 * 10);
    }

    private int getCursorIndex(Cursor cursor, String columnName) {
        return cursor.getColumnIndex(columnName);
    }

    public Species selectSpeciesByCode(String code) {
        String select = String.format("SELECT * FROM species where code=\"%s\" order by id", code);
        Species species = new Species();
        try (Cursor cursor = db.rawQuery(select, null)) {
            while (cursor.moveToNext()) {
                species.setId(cursor.getInt(getCursorIndex(cursor, "id")));
                species.setCode(cursor.getString(getCursorIndex(cursor, "code")));
                species.setName(cursor.getString(getCursorIndex(cursor, "name")));
                species.setLatin(cursor.getString(getCursorIndex(cursor, "latin")));
                species.setPlant(cursor.getString(getCursorIndex(cursor, "plant")));
                species.setArea(cursor.getString(getCursorIndex(cursor, "area")));
                species.setImage(cursor.getString(getCursorIndex(cursor, "image")));
                species.setGenus_name(cursor.getString(getCursorIndex(cursor, "genus_name")));
                species.setFamily_name(cursor.getString(getCursorIndex(cursor, "family_name")));
                species.setOrder_name(cursor.getString(getCursorIndex(cursor, "order_name")));
            }
        }
        return species;
    }

    private boolean isIdExist(int id) {
        String sql = "select COUNT(*) as count from species where id=" + id;
        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor.moveToNext()) {
                return cursor.getInt(getCursorIndex(cursor, "count")) == 1;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void syncFromRemote() {
        OkHttpClient okHttpClient = new OkHttpClient();
        int pageNum = 1;
        final int pageSize = 10;
        int pages = 1;
        while (true) {
            Request build = new Request.Builder().url(this.remoteApiUrl + "?pageNum=" + pageNum + "&pageSize=" + pageSize).build();
            try (Response resp = okHttpClient.newCall(build).execute()) {
                ResponseBody body = resp.body();
                if (body != null) {
                    String respStr = body.string();
                    Map<String, Object> map = JSON.parseObject(respStr, Map.class);
                    Map<String, Object> result = (Map<String, Object>) map.get("result");
                    try {
                        List<Object> data = (List<Object>) result.get("data");
                        pages = (int) result.get("pages");
                        for (Object each : data) {
                            try {
                                db.execSQL("BEGIN;");
                                Map<String, Object> speciesInfo = (Map<String, Object>) each;
                                int id = (int) speciesInfo.get("id");
                                if (isIdExist(id)) {
                                    ContentValues contentValues = new ContentValues();
                                    for (String key : speciesInfo.keySet()) {
                                        Object o = speciesInfo.get(key);
                                        if (o instanceof String) {
                                            contentValues.put(key, (String) o);
                                        }
                                    }
                                    db.update("species", contentValues, "id=?", new String[]{String.valueOf(id)});
                                } else {
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put("id", id);
                                    contentValues.put("code", (String) speciesInfo.get("code"));
                                    contentValues.put("name", (String) speciesInfo.get("name"));
                                    contentValues.put("latin", (String) speciesInfo.get("latin"));
                                    contentValues.put("plant", (String) speciesInfo.get("plant"));
                                    contentValues.put("area", (String) speciesInfo.get("area"));
                                    contentValues.put("image", (String) speciesInfo.get("image"));
                                    contentValues.put("genus_name", (String) speciesInfo.get("genus_name"));
                                    contentValues.put("family_name", (String) speciesInfo.get("family_name"));
                                    contentValues.put("order_name", (String) speciesInfo.get("order_name"));
                                    db.insert("species", null, contentValues);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                db.execSQL("COMMIT;");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    pageNum++;
                    if (pageNum >= pages) {
                        break;
                    }
                } else {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void close() {
        this.db.close();
        this.timer.cancel();
    }
}
