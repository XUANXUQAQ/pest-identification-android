package com.rjgc.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片转换工具类
 */
public class Base64Utils {

    public static void base642Jpg(String base64, String savePath) {
        Bitmap bitmap;
        try {
            byte[] bitmapArray = Base64.decode(base64, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
            savePNG(bitmap, savePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void savePNG(Bitmap bitmap, String savePath) {
        File file = new File(savePath);
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
