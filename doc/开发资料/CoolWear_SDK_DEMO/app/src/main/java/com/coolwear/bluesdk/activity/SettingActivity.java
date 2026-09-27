package com.coolwear.bluesdk.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.coolwear.bluesdk.R;

import ce.com.cenewbluesdk.BleContentProvider.CEBlueSharedPreference;


public class SettingActivity extends Activity {

    EditText edit_query,edit_num ,edit_con_timeout;
    Button btn_sure;

    String name;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_name);
        edit_query = findViewById(R.id.edit_query);
        edit_num = findViewById(R.id.edit_num);
        edit_con_timeout = findViewById(R.id.edit_con_timeout);
        edit_query.setText(CEBlueSharedPreference.getDevUUId());
        btn_sure = findViewById(R.id.btn_sure);
//        edit_query.setText( SharedPreferenceUtils.getInstance(this).getFilterName());
//        edit_num.setText( SharedPreferenceUtils.getInstance(this).getConnNum()+"");
//        edit_con_timeout.setText(SharedPreferenceUtils.getInstance(this).getConnTimeOut()+"");
        btn_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                String numStr = edit_num.getText().toString();
//
//                if(numStr == null){
//                    Toast.makeText(SettingActivity.this,"请输入同时连接个数",Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                int num = 0;
//                try {
//                     num = Integer.parseInt(numStr.trim());
//
//                     if(num>7||num<1){
//                         Toast.makeText(SettingActivity.this,"请输入1~7的数字",Toast.LENGTH_SHORT).show();
//                         return;
//                     }
//
//                }catch (Exception e){
//                    e.printStackTrace();
//                    Toast.makeText(SettingActivity.this,"请输入正确的数字",Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                String timeOut = edit_con_timeout.getText().toString();
//                if(timeOut == null){
//                    Toast.makeText(SettingActivity.this,"请输入扫描间隔时间",Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                int timeout = 0;
//                try {
//                    timeout = Integer.parseInt(timeOut.trim());
//
//                    if(timeout>12000||timeout<2000){
//                        Toast.makeText(SettingActivity.this,"请输入2000~12000的数字",Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                }catch (Exception e){
//                    e.printStackTrace();
//                    Toast.makeText(SettingActivity.this,"请输入正确的数字",Toast.LENGTH_SHORT).show();
//                    return;
//                }

//                SharedPreferenceUtils.getInstance(SettingActivity.this).setFilterName(edit_query.getText().toString());
//                SharedPreferenceUtils.getInstance(SettingActivity.this).setConnNum(num);
//                SharedPreferenceUtils.getInstance(SettingActivity.this).setConnTimeOut(timeout);
                CEBlueSharedPreference.setDevUUid(edit_query.getText().toString());
                finish();
            }
        });
    }
}
