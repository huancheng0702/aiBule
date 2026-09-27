package com.coolwear.bluesdk.activity;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.coolwear.bluesdk.R;
import com.coolwear.bluesdk.utils.FileUtils2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import pub.devrel.easypermissions.EasyPermissions;

public class OtaImportFileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota_import_file);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = intent.getData();
        if (uri != null && uri.getPath() != null && Intent.ACTION_VIEW.equals(action) && uri.getPath().endsWith(".img")) {
            String[] storagePerms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            if (!EasyPermissions.hasPermissions(this, storagePerms)) {
                EasyPermissions.requestPermissions(this, "Request storage permission", 0, storagePerms);
            }
            saveInputStream2File(intent.getData());
        } else {
            TextView tipsTV = findViewById(R.id.tips_tv);
            tipsTV.setText("文件导入失败");
        }

        Button confirmBtn = findViewById(R.id.confirm_btn);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApp();
            }
        });
    }

    @Override
    public void onBackPressed() {
        restartApp();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    private void saveInputStream2File(Uri uri) {
        InputStream is;
        TextView tipsTV = findViewById(R.id.tips_tv);
        String tips;
        try {
            is = getContentResolver().openInputStream(uri);
            //Convert your stream to data here
            //String filePath = FileUtils.getSDPath() + "/YFit-OTA/" + uri.getLastPathSegment();
            // Android Q 对外部存储作出了限制，此处文件直接写入app的沙盒路径
//            String filePath = getFilesDir().getAbsolutePath() + "/" + uri.getLastPathSegment();
            File filePath = new File(getExternalFilesDir("IMG_FILE"),uri.getLastPathSegment());
            FileUtils2.inputStream2file(is, filePath);
            if (is != null) {
                is.close();
            }

            tips = this.getString(R.string.tipsImportSuccess);
        } catch (IOException e) {
            e.printStackTrace();
            tips = this.getString(R.string.tipsImportFailed);
        }
        tipsTV.setText(tips);
    }

    private void restartApp() {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }
}
