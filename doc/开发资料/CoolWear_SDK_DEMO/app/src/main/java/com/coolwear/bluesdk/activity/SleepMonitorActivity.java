package com.coolwear.bluesdk.activity;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.databinding.ActivitySleepMonitorBinding;
import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;
import ce.com.cenewbluesdk.entity.k6.DateTimeUtil;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;

public class SleepMonitorActivity extends AppCompatActivity {
    private ActivitySleepMonitorBinding monitorBinding;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        monitorBinding = ActivitySleepMonitorBinding.inflate(getLayoutInflater());
        setContentView(monitorBinding.getRoot());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(1);
        BluetoothManager.sleepMonitorLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                monitorBinding.tvRevTime.setText(DateTimeUtil.getCurrDateTimeStrMill());
            }
        });
        monitorBinding.tvMac.setText("mac:" + CEBlueSharedPreference.getDevAddress());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(0);
    }

}
