package com.coolwear.bluesdk.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.view.ContactsAdapter;

import java.util.List;

import ce.com.cenewbluesdk.entity.k6.K6_DATA_TYPE_CONTACT_SYNC;
import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;

import static com.coolwear.bluesdk.BluetoothManager.contactLiveData;
import static com.coolwear.bluesdk.BluetoothManager.delContactLiveData;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/9/9 0009 14:15
 */
public class ContactsActivity extends AppCompatActivity {
    private Button bt_sure;
    private Button bt_clear;
    private EditText et_name;
    private EditText et_number;
    private RecyclerView recyclerView;
    private ContactsAdapter adapter;
    private int delPosition;
    private String tempName;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bt_sure = findViewById(R.id.bt_sure);
        bt_clear = findViewById(R.id.bt_clear);
        et_name = findViewById(R.id.et_name);
        et_number = findViewById(R.id.et_number);
        recyclerView = findViewById(R.id.recyclerView);
        adapter = new ContactsAdapter(this);
        recyclerView.setAdapter(adapter);

        et_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                tempName = charSequence.toString();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (TextUtils.isEmpty(editable.toString())) return;
                if (editable.toString().getBytes().length > 20) {
                    et_name.setText(tempName);
                }
            }
        });

        bt_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothHelper.getInstance().getSendBlueData().sendClearContacts();
            }
        });
        bt_sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(et_name.getText().toString().trim())) {
                    Toast.makeText(ContactsActivity.this, "请输入姓名", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(et_number.getText().toString().trim())) {
                    Toast.makeText(ContactsActivity.this, "请输入号码", Toast.LENGTH_SHORT).show();
                    return;
                }
                BluetoothHelper.getInstance().getSendBlueData().sendAddContacts(et_name.getText().toString().trim(), et_number.getText().toString().trim());
            }
        });
        adapter.setOnItemClickListener(new ContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClickListener(int position) {
                delPosition = position;
//                BluetoothHelper.getInstance().getSendBlueData().sendDelContacts(adapter.getItem(position).getPosition());
                BluetoothHelper.getInstance().getSendBlueData().sendDelContacts(position+1);

            }
        });
        contactLiveData.observe(this, new Observer<K6_DATA_TYPE_CONTACT_SYNC>() {
            @Override
            public void onChanged(K6_DATA_TYPE_CONTACT_SYNC k6_data_type_contact_syncs) {
                if (k6_data_type_contact_syncs != null && k6_data_type_contact_syncs.getPosition() > 0) {
                    adapter.addItem(k6_data_type_contact_syncs);
                    BluetoothHelper.getInstance().getSendBlueData().sendContactsSync(k6_data_type_contact_syncs.getPosition());
                } else {
                    Toast.makeText(ContactsActivity.this,"同步完成",Toast.LENGTH_LONG).show();
                }
            }
        });
        delContactLiveData.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isDel) {
                if(isDel){
                    adapter.removeItem(delPosition);
//                    Toast.makeText(ContactsActivity.this,"删除成功",Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(ContactsActivity.this,"删除失败",Toast.LENGTH_LONG).show();
                }
            }
        });
        BluetoothHelper.getInstance().getSendBlueData().sendContactsSync(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!BluetoothHelper.getInstance().isConnectOk()){
            Toast.makeText(ContactsActivity.this,"蓝牙断开",Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
