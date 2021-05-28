package com.rjgc;

import android.content.Context;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            cnnApi.init(assetFilePath(this, "model.pt"));
            new LocalNetApi(8888, this.getCacheDir(), cnnApi).start();
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
                System.exit(0);
            }
        }
    }

    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    private static String assetFilePath(Context context, @SuppressWarnings("SameParameterValue") String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}
