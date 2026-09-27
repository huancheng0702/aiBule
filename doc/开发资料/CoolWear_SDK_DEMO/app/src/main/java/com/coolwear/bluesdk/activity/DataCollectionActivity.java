package com.coolwear.bluesdk.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.coolwear.bluesdk.BluetoothManager;
import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.bean.CheckDataTypeBean;
import com.coolwear.bluesdk.databinding.ActivityDataCollectionBinding;
import com.coolwear.bluesdk.tcp.TcpClientService;
import com.coolwear.bluesdk.utils.ToastUtil;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.util.TextUtils;

import ce.com.cenewbluesdk.entity.k6.DateTimeUtil;
import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_FUNCTION_CONTROL;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;

/**
 * 数据采集 第1步
 */
public class DataCollectionActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityDataCollectionBinding binding;
    private Activity mActivity;
    private int serverType = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        mActivity = this;
        binding = ActivityDataCollectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.btnStart.setOnClickListener(this);
        //BluetoothHelper.getInstance().getSendBlueData().sendSwitchGSensor(1);
        binding.tvInfo.setText(DateTimeUtil.getCurrDateTimeStrMill() + "\n");
        binding.etUserName.setText(BluetoothHelper.getInstance().getPersistent().getUserId());
//        BluetoothManager.gSensorLiveData.observe(this, new Observer<k6_DATA_TYPE_MOTION_DATA>() {
//            @Override
//            public void onChanged(k6_DATA_TYPE_MOTION_DATA dataTypeMotionData) {
//                binding.tvInfo.setText(DateTimeUtil.getCurrDateTimeStrMill() + "\n" + dataTypeMotionData.getSpeed_Throw() + "," + dataTypeMotionData.getX_Move_Distance() + "," + dataTypeMotionData.getY_Move_Distance());
//            }
//        });
//
//        BluetoothManager.sportData.observe(this, new Observer<List<K6_Sport>>() {
//            @Override
//            public void onChanged(List<K6_Sport> sportData) {
//                if (sportData != null && sportData.size() > 0) {
//                    binding.tvStep.setText("步数:  " + sportData.get(sportData.size() - 1).getWalkSteps() + "步");
//                    binding.tvDistance.setText("距离:  " + sportData.get(sportData.size() - 1).getDistance() + "米");
//                    binding.tvCalories.setText("卡路里:  " + sportData.get(sportData.size() - 1).getCalories() / 1000 + "千卡");
////                    String s = gSenSorBinding.tvStep.getText().toString();
////                    String d = gSenSorBinding.tvDistance.getText().toString();
////                    String c = gSenSorBinding.tvCalories.getText().toString();
////                    Lg.e_0316("BluetoothManager", dataTypeMotionData.getSpeed_Throw() +"," + dataTypeMotionData.getX_Move_Distance() + "," + dataTypeMotionData.getY_Move_Distance()  ,"GsenSor.log",true);
//
//                }
//
//            }
//        });

        BluetoothManager.controlLiveData.observe(this, new Observer<K6_DATA_TYPE_FUNCTION_CONTROL>() {
            @Override
            public void onChanged(K6_DATA_TYPE_FUNCTION_CONTROL k6_data_type_function_control) {
                if (k6_data_type_function_control.isHasGame()) {
                    binding.cbGs.setVisibility(View.VISIBLE);
                } else {
                    binding.cbGs.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start:
                hideKeyBoard(mActivity);
                if (TextUtils.isEmpty(binding.etUserName.getText())) {
                    ToastUtil.showToast(mActivity, "请输入用户名！");
                    return;
                }
                if (R.id.rd_1 == binding.rg1.getCheckedRadioButtonId()) {
                    serverType = 1;
                } else {
                    serverType = 2;
                }

                if (!binding.cbHr.isChecked() &&  !binding.cbGs.isChecked() && !binding.cbPower.isChecked()) {
                    ToastUtil.showToast(mActivity, "请选择数据类型");
                    return;
                }
                if(binding.cbHr.isChecked() && binding.cbGs.isChecked()){
                    ToastUtil.showToast(mActivity, "心率和Gsensor只能选一种！");
                    return;
                }
                CheckDataTypeBean checkDataTypeBean = new CheckDataTypeBean();
                checkDataTypeBean.setHrIsCheck(binding.cbHr.isChecked());
                checkDataTypeBean.setGsIsCheck(binding.cbGs.isChecked());
                checkDataTypeBean.setBatteryIsCheck(binding.cbPower.isChecked());
                checkDataTypeBean.setUserName(binding.etUserName.getText().toString());
                Bundle bundle = new Bundle();
                BluetoothHelper.getInstance().getPersistent().setUserId(checkDataTypeBean.getUserName());
                if (binding.cbGs.getVisibility() == View.VISIBLE) {
                    bundle.putBoolean(TcpClientService.GS_IS_CHECKED_KEY, binding.cbGs.isChecked());
                }
                bundle.putInt(TcpClientService.SERVER_TYPE_KEY, serverType);
                bundle.putSerializable(TcpClientService.CHECK_DATA_TYPE_KEY, checkDataTypeBean);
                Intent intent = new Intent(mActivity, DataCollectionDetailsActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);

                Intent tcpService = new Intent(this, TcpClientService.class);
                tcpService.putExtras(bundle);
                startService(tcpService);
                break;
        }
    }

    public static void hideKeyBoard(Activity aty) {
        final View currentFocus = aty.getCurrentFocus();
        if (currentFocus != null)
            ((InputMethodManager) aty
                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(
                            currentFocus.getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
    }
}
