package com.rjgc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

import com.fruitbasket.R;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;
import com.getcapacitor.ui.Toast;
import com.rjgc.api.CnnApi;
import com.rjgc.api.LocalDatabaseApi;
import com.rjgc.api.LocalNetApi;
import com.rjgc.sqlite.SqliteUtils;
import com.rjgc.utils.ThreadPool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
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
            new LocalDatabaseApi(8889, this, "http://120.79.146.15:8081/species/all").start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onStart() {
        super.onStart();
        File offlineDatabase = new File(this.getCacheDir(), "offlineDatabase");
        if (!offlineDatabase.exists()) {
            AlertDialog syncDialog = buildDialog("是否同步云端数据库，否则无法使用离线功能", offlineDatabase);
            syncDialog.show();
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(offlineDatabase))) {
                String s = reader.readLine();
                LocalDateTime time = LocalDateTime.parse(s);
                long days = Duration.between(LocalDateTime.now(), time).toDays();
                if (days > 5) {
                    AlertDialog syncDialog = buildDialog("是否更新本地数据库并与云端同步", offlineDatabase);
                    syncDialog.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertDialog syncDialog = buildDialog("是否同步云端数据库，否则无法使用离线功能", offlineDatabase);
                syncDialog.show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private AlertDialog buildDialog(String msg, File offlineDatabase) {
        return new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage(msg)
                .setPositiveButton("确定", (dialog, which) -> {
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setMessage("正在同步中");
                    progressDialog.show();
                    ThreadPool.INSTANCE.execute(() -> {
                        try {
                            SqliteUtils.getInstance().syncFromRemote();
                            try (BufferedWriter writer = new BufferedWriter(new FileWriter(offlineDatabase))) {
                                writer.write(String.valueOf(LocalDateTime.now()));
                            }
                            this.runOnUiThread(() -> Toast.show(this, "同步完成"));
                        } catch (Exception e) {
                            this.runOnUiThread(() -> Toast.show(this, "同步失败"));
                            e.printStackTrace();
                        } finally {
                            progressDialog.dismiss();
                        }
                    });
                })
                .setNegativeButton("取消", ((dialog, which) -> Toast.show(this, "离线功能将无法使用")))
                .create();
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
                SqliteUtils.getInstance().close();
                ThreadPool.INSTANCE.shutdown();
                System.exit(0);
            }
        }
    }
}
