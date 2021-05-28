package com.rjgc;

import android.os.Bundle;
import android.webkit.WebView;

import com.fruitbasket.R;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;
import com.getcapacitor.ui.Toast;
import com.rjgc.api.CnnApi;
import com.rjgc.api.LocalDatabaseApi;
import com.rjgc.api.LocalNetApi;
import com.rjgc.sqlite.SqliteUtils;
import com.rjgc.utils.ThreadPool;

import java.io.File;
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
        init();
    }

    private void init() {
        try {
            CnnApi cnnApi = CnnApi.getInstance();
            cnnApi.init(getAssets(), "model.pt");
            new LocalNetApi(8888, this.getCacheDir()).start();
            new LocalDatabaseApi(8889, this, "http://rjgc.club:8099/species/all").start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        File firstSign = new File(this.getCacheDir(), "first");
        if (!firstSign.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                firstSign.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.show(this, "检测到第一次使用，正在同步数据库，否则将无法使用离线功能");
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
                Toast.show(this, "再按一次退出程序");
                mPressedTime = mNowTime;
            } else {//退出程序
                this.finish();
                SqliteUtils.getInstance(null, "").close();
                ThreadPool.INSTANCE.shutdown();
                System.exit(0);
            }
        }
    }
}
