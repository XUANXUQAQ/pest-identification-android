package com.rjgc.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.getcapacitor.ui.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "create table species\n" +
                "(\n" +
                "    \"id\",\n" +
                "    \"code\",\n" +
                "    \"name\",\n" +
                "    \"latin\",\n" +
                "    \"plant\",\n" +
                "    \"area\",\n" +
                "    \"image\",\n" +
                "    \"genus_name\",\n" +
                "    \"family_name\",\n" +
                "    \"order_name\"\n" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
