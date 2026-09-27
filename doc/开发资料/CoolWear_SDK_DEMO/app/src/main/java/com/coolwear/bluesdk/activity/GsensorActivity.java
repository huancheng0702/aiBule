package com.coolwear.bluesdk.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.databinding.ActivityGsensorBinding;
import com.coolwear.bluesdk.databinding.ActivityMainBinding;

import java.util.List;

import ce.com.cenewbluesdk.entity.k6.DateTimeUtil;
import ce.com.cenewbluesdk.entity.k6.K6_Sport;
import ce.com.cenewbluesdk.entity.k6.k6_DATA_TYPE_MOTION_DATA;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
import ce.com.cenewbluesdk.uitl.Lg;

public class GsensorActivity extends AppCompatActivity {
    private ActivityGsensorBinding gSenSorBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gSenSorBinding = ActivityGsensorBinding.inflate(getLayoutInflater());
        setContentView(gSenSorBinding.getRoot());
        BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(1);
        gSenSorBinding.tvInfo.setText(DateTimeUtil.getCurrDateTimeStrMill() + "\n");
        BluetoothManager.gSensorLiveData.observe(this, new Observer<k6_DATA_TYPE_MOTION_DATA>() {
            @Override
            public void onChanged(k6_DATA_TYPE_MOTION_DATA dataTypeMotionData) {
                gSenSorBinding.tvInfo.setText(DateTimeUtil.getCurrDateTimeStrMill() + "\n" + dataTypeMotionData.getSpeed_Throw() + "," + dataTypeMotionData.getX_Move_Distance() + "," + dataTypeMotionData.getY_Move_Distance());
                for (k6_DATA_TYPE_MOTION_DATA data : dataTypeMotionData.getList()) {
                  //  Lg.e_0316("BluetoothManager", data.getSpeed_Throw() + "," + data.getX_Move_Distance() + "," + data.getY_Move_Distance(), "GsenSor.log", true);
                }

            }
        });

        BluetoothManager.sportData.observe(this, new Observer<List<K6_Sport>>() {
            @Override
            public void onChanged(List<K6_Sport> sportData) {
                if (sportData != null && sportData.size() > 0) {
                    gSenSorBinding.tvStep.setText("步数:  " + sportData.get(sportData.size() - 1).getWalkSteps() + "步");
                    gSenSorBinding.tvDistance.setText("距离:  " + sportData.get(sportData.size() - 1).getDistance() + "米");
                    gSenSorBinding.tvCalories.setText("卡路里:  " + sportData.get(sportData.size() - 1).getCalories() / 1000 + "千卡");
//                    String s = gSenSorBinding.tvStep.getText().toString();
//                    String d = gSenSorBinding.tvDistance.getText().toString();
//                    String c = gSenSorBinding.tvCalories.getText().toString();
//                    Lg.e_0316("BluetoothManager", dataTypeMotionData.getSpeed_Throw() +"," + dataTypeMotionData.getX_Move_Distance() + "," + dataTypeMotionData.getY_Move_Distance()  ,"GsenSor.log",true);

                }

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(0);
    }
}
