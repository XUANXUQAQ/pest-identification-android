package com.fruitbasket.audioplatform;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.fruitbasket.R;
import com.fruitbasket.audioplatform.api.LocalDatabaseApi;
import com.fruitbasket.audioplatform.api.LocalNetApi;
import com.fruitbasket.audioplatform.utils.sqlite.SqliteUtils;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends BridgeActivity {
    private long mPressedTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initializes the Bridge
        this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {
        });
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python python = Python.getInstance();
        PyObject cnn = python.getModule("cnn");
        // 初始化神经网络
        // todo 测试删除 http://183.195.216.150:8899/classes
        cnn.callAttr("init", "http://example.com");
        try {
            new LocalNetApi(8888, this.getCacheDir(), cnn).start();
            new LocalDatabaseApi(8889, this, "http://rjgc.club:8099/species/all").start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        WebView webView = findViewById(R.id.webview);
        String url = webView.getUrl();
        if (url.contains("Picture") || url.contains("Video") || url.contains("Entry")) {
            super.onBackPressed();
        } else {
            long mNowTime = System.currentTimeMillis();//获取第一次按键时间
            if ((mNowTime - mPressedTime) > 2000) {//比较两次按键时间差
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mPressedTime = mNowTime;
            } else {//退出程序
                this.finish();
                SqliteUtils.getInstance(null, "").close();
                System.exit(0);
            }
        }
    }
}
