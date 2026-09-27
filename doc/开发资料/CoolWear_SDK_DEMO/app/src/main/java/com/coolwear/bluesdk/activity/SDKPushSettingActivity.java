package com.coolwear.bluesdk.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coolwear.bluesdk.MyApplication;
import com.coolwear.bluesdk.view.PushListAdapter;
import com.coolwear.bluesdk.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ce.com.cenewbluesdk.CEBC;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.proxy.interfaces.OnPushAppListListener;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/23 0023 17:36
 */
public class SDKPushSettingActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private Button btn_notification;
    private PushListAdapter adapter;
    private boolean isNtfEnabled;
    private ProgressBar progressBar;

    //    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            List<HashMap> tAppList = adapter.getInstallApps();
//            BluetoothHelper.getInstance().getPersistent().setAppDevicePushMessage(tAppList);
//            finish();
//        }
//        return super.onOptionsItemSelected(item);
//    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sdkpushsetting);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        recyclerView = findViewById(R.id.recyclerView);
        btn_notification = findViewById(R.id.btn_notification);
        progressBar = findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
        adapter = new PushListAdapter(this);
        recyclerView.setAdapter(adapter);

//        loadData();
        btn_notification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openNotificationListenSettings(SDKPushSettingActivity.this);
            }
        });

    }

    public static void openNotificationListenSettings(Activity activity) {
        try {
            Intent intent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            } else {
                intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            }
            activity.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<HashMap> mAppLists = BluetoothHelper.getInstance().getPersistent().loadPushAppList(MyApplication.getInstance());
                SDKPushSettingActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setInstallApps(mAppLists);
                    }
                });
            }
        }).start();

        //这是获取手机所有的安装应用
        BluetoothHelper.getInstance().getPersistent().getSdkPushSetting(this, new OnPushAppListListener() {
            @Override
            public void onPushAppList(List<HashMap> mAppLists) {
//                progressBar.setVisibility(View.GONE);
//                recyclerView.setVisibility(View.VISIBLE);
//                adapter.setInstallApps(mAppLists);
            }

            @Override
            public void onPushAppError(Exception e) {
//                progressBar.setVisibility(View.GONE);
//                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isNtfEnabled = isNotificationListenerEnabled(this);
        if (isNtfEnabled) {
            btn_notification.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            loadData();
        } else {
            btn_notification.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    public static boolean isNotificationListenerEnabled(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        if (packageNames.contains(context.getPackageName())) {
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        List<HashMap> tAppList = adapter.getInstallApps();
        BluetoothHelper.getInstance().getPersistent().setAppDevicePushMessage(tAppList);
    }
}
