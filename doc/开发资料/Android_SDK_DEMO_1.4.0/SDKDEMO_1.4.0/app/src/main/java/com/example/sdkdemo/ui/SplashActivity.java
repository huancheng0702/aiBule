package com.example.sdkdemo.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.sdkdemo.R;


public class SplashActivity extends AppCompatActivity {

    private View tvStartBind;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 内容延伸到状态栏、导航栏区域
        WindowCompat.setDecorFitsSystemWindows(
                getWindow(),
                false
        );

        setContentView(R.layout.activity_splash);

        initStatusBar();

        initView();
    }

    /**
     * 设置透明系统栏
     */
    private void initStatusBar() {

        Window window = getWindow();

        // 状态栏透明
        window.setStatusBarColor(
                Color.TRANSPARENT
        );

        // 导航栏透明
        window.setNavigationBarColor(
                Color.TRANSPARENT
        );

        // 状态栏使用白色图标
        WindowCompat.getInsetsController(
                window,
                window.getDecorView()
        ).setAppearanceLightStatusBars(false);

        // 导航栏使用白色图标
        WindowCompat.getInsetsController(
                window,
                window.getDecorView()
        ).setAppearanceLightNavigationBars(false);
    }

    private void initView() {

        tvStartBind =
                findViewById(R.id.tv_start_bind);

        tvStartBind.setOnClickListener(v -> {

            // TODO 开始绑定设备

            startBindDevice();
        });
    }

    /**
     * 开始绑定设备
     */
    private void startBindDevice() {

        // 这里进入 BLE 扫描或者绑定流程
        startActivity(new Intent(this, BleScanActivity.class));
    }
}