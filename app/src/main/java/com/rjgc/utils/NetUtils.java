package com.rjgc.utils;

import android.graphics.Bitmap;

import com.rjgc.api.CnnApi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public enum NetUtils {
    INSTANCE;

    NetUtils() {
        init();
    }

    // Object[]第一个为code(HashMap<String, Integer>)，第二个为处理后的图像
    private final ConcurrentHashMap<Bitmap, Object[]> map = new ConcurrentHashMap<>();

    public void send(Bitmap bitmap) {
        map.put(bitmap, new Object[] {new HashMap<>(), null});
    }

    // Object[]第一个为code(HashMap<String, Integer>)，第二个为处理后的图像
    public Object[] getResult(Bitmap bitmap) {
        long start = System.currentTimeMillis();
        Object[] res = map.get(bitmap);
        try {
            HashMap<String, Integer> tmp = (HashMap<String, Integer>) res[0];
            while (tmp.isEmpty()) {
                res = map.get(bitmap);
                tmp = (HashMap<String, Integer>) res[0];
                if (System.currentTimeMillis() - start > 5000) {
                    System.err.println("等待超时");
                    break;
                }
                TimeUnit.MICROSECONDS.sleep(10);
            }
        } catch (InterruptedException ignored) {
        }
        map.remove(bitmap);
        return res;
    }

    private void init() {
        ThreadPool.INSTANCE.execute(() -> {
            CnnApi cnnApi = CnnApi.getInstance();
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    for (Bitmap each : map.keySet()) {
                        HashMap<String, Integer> res = (HashMap<String, Integer>) map.get(each)[0];
                        assert res != null;
                        if (res.isEmpty()) {
                            Object[] processedImg = new Object[1];
                            Map<String, Integer> predict = cnnApi.predict(each, processedImg);
                            map.put(each, new Object[] {predict, processedImg[0]});
                        }
                    }
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
            } catch (InterruptedException ignored) {
            }
        });
    }
}
