package com.rjgc.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SqliteUtils {

    private final SQLiteDatabase db;
    private final String remoteApiUrl;
    private static volatile SqliteUtils instance;
    public static volatile boolean isCreated = false;

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

    public static SqliteUtils getInstance() {
        if (isCreated) {
            return instance;
        }
        throw new RuntimeException("还未初始化");
    }

    private SqliteUtils(Context context, String remoteApiUrl) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context, "pest-identification.db", null, 1);
        this.db = databaseHelper.getWritableDatabase();
        this.remoteApiUrl = remoteApiUrl;
        isCreated = true;
    }

    private int getCursorIndex(Cursor cursor, String columnName) {
        return cursor.getColumnIndex(columnName);
    }

    /**
     * 根据code查询害虫信息，模拟远程api
     * @param code 害虫code
     * @return 害虫信息
     */
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

    /**
     * 从远程数据库同步，保存到本地数据库
     */
    public void syncFromRemote() {
        OkHttpClient okHttpClient = new OkHttpClient();
        AtomicInteger pageNum = new AtomicInteger(1);
        final int pageSize = 5;
        AtomicInteger pages = new AtomicInteger(1);
        final boolean[] isPagesUpdateFromRemote = {false};
        AtomicInteger finishedNum = new AtomicInteger(0);
        AtomicInteger totalNum = new AtomicInteger(0);

        String url = this.remoteApiUrl + "?pageNum=" + pageNum.get() + "&pageSize=" + pageSize;
        Request build = new Request.Builder().url(url).build();
        Call call = okHttpClient.newCall(build);
        totalNum.incrementAndGet();
        enqueue(call, pages, isPagesUpdateFromRemote, finishedNum);
        pageNum.incrementAndGet();

        //等待网络连接
        long start = System.currentTimeMillis();
        while (!isPagesUpdateFromRemote[0]) {
            if (System.currentTimeMillis() - start > 5000) {
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (isPagesUpdateFromRemote[0]) {
            while (pageNum.get() <= pages.get()) {
                url = this.remoteApiUrl + "?pageNum=" + pageNum.get() + "&pageSize=" + pageSize;
                build = new Request.Builder().url(url).build();
                call = okHttpClient.newCall(build);
                totalNum.incrementAndGet();
                enqueue(call, pages, isPagesUpdateFromRemote, finishedNum);
                pageNum.incrementAndGet();
            }
        }

        try {
            start = System.currentTimeMillis();
            while (finishedNum.get() != totalNum.get() || finishedNum.get() == 0) {
                if (System.currentTimeMillis() - start > 60000) {
                    System.err.println("sync timeout");
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(50);
            }
        } catch (InterruptedException ignored) {
        }
        System.out.println("sync finished");
    }

    @SuppressWarnings("unchecked")
    private void enqueue(Call call, AtomicInteger pages, boolean[] isPagesUpdateFromRemote, AtomicInteger finishedNum) {
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                finishedNum.incrementAndGet();
            }

            @Override
            public void onResponse(Call call, Response resp) throws IOException {
                ResponseBody body = resp.body();
                if (body == null) {
                    return;
                }
                String respStr = body.string();
                Map<String, Object> map = JSON.parseObject(respStr, Map.class);
                Map<String, Object> result = (Map<String, Object>) map.get("result");
                try {
                    db.execSQL("BEGIN;");
                    Object data1 = result.get("data");
                    List<Object> data;
                    if (data1 instanceof String) {
                        data = JSON.parseArray((String) data1);
                    } else {
                        data = (List<Object>) data1;
                    }
                    pages.set((int) result.get("pages"));
                    isPagesUpdateFromRemote[0] = true;
                    for (Object each : data) {
                        try {
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
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    db.execSQL("COMMIT;");
                    finishedNum.incrementAndGet();
                }
            }
        });
    }

    public void close() {
        this.db.close();
    }
}
