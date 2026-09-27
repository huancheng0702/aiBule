//package com.coolwear.bluesdk.activity;
//
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Color;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.text.TextUtils;
//import android.view.MenuItem;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ImageView;
//import android.widget.RadioButton;
//import android.widget.RadioGroup;
//import android.widget.Spinner;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.lifecycle.Observer;
//
//import com.coolwear.bluesdk.BluetoothManager;
//import com.coolwear.bluesdk.Constant;
//import com.coolwear.bluesdk.R;
//import com.coolwear.bluesdk.bean.ClockDialData;
//import com.coolwear.bluesdk.utils.FileUtils2;
//import com.kaopiz.kprogresshud.KProgressHUD;
//import com.yalantis.ucrop.UCrop;
//import com.yalantis.ucrop.UCropActivity;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//
//import ce.com.cenewbluesdk.entity.ModifyWatchInfo;
//import ce.com.cenewbluesdk.proxy.interfaces.OnCreateBinFileListener;
//import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper;
//
//import static com.yalantis.ucrop.UCrop.REQUEST_CROP;
//
///**
// * @Description:
// * @Author: zhengyunbin
// * @CreateDate: 2021/3/24 0024 14:55
// */
//public class ModifyDialActivity extends AppCompatActivity {
//
//    private Spinner spinner_color;
//    private TextView imageInfoTextView;
//    private ImageView dialImageView;
//    private RadioButton rb_defaultimg, rb_customimg;
//    private RadioGroup rg_img;
//    private boolean isChangePic = true;
//    private int imageStatus;
//    private int radioImg;
//    private int selectedImageResultCode = 1000;
//    private ModifyWatchInfo modifyWatchInfo = new ModifyWatchInfo();
//    private int selectedImageWith = 0;
//    private int selectedImageHeight = 0;
//    private int firstRadio;
//    private String watchData = "";
//    private KProgressHUD hud;
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            finish();
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//    }
//
//    public static Bitmap getLoacalBitmap(String url) {
//        try {
//            FileInputStream fis = new FileInputStream(url);
//            return BitmapFactory.decodeStream(fis);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_modifydial);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        spinner_color = findViewById(R.id.spinner_color);
//        imageInfoTextView = findViewById(R.id.tv_image_info);
//        dialImageView = findViewById(R.id.iv_dial);
//        rb_defaultimg = findViewById(R.id.rb_defaultimg);
//        rb_customimg = findViewById(R.id.rb_customimg);
//        rg_img = findViewById(R.id.rg_img);
//        modifyWatchInfo.setSavePath(FileUtils2.getCustomPath(Constant.customId));
//        hud = KProgressHUD.create(this, KProgressHUD.Style.BAR_DETERMINATE)
//                .setLabel("Please wait")
//                .setMaxProgress(100);
//        spinner_color.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
//                modifyWatchInfo.setColor(ClockDialData.TEXT_COLORS.get(position).getColor());
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });
//        rg_img = findViewById(R.id.rg_img);
//        rg_img.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int position) {
//                switch (group.getCheckedRadioButtonId()) {
//                    case R.id.rb_defaultimg:
//                        if (radioImg == position) {
//                            isChangePic = false;
//                        } else {
//                            isChangePic = true;
//                        }
//                        imageStatus = 1;
//                        radioImg = position;
//                        modifyWatchInfo.setFilePath("");
//                        dialImageView.setImageResource(R.mipmap.icon_dial_0_default);
//                        break;
//                    case R.id.rb_customimg:
//                        chooseImageFromGallery();
//                        break;
//                }
//            }
//        });
//        radioImg = rg_img.getCheckedRadioButtonId();
//        firstRadio = rg_img.getCheckedRadioButtonId();
//        //时间位置
//        ((RadioGroup) findViewById(R.id.rg_time_pos)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int i) {
//                switch (group.getCheckedRadioButtonId()) {
//                    case R.id.rb_up:
//                        modifyWatchInfo.setTime_pos(ClockDialData.TIME_POSITIONS.get(0).getValue());
//                        break;
//                    case R.id.rb_down:
//                        modifyWatchInfo.setTime_pos(ClockDialData.TIME_POSITIONS.get(1).getValue());
//                        break;
//                }
//            }
//        });
//        //时间上方
//        ((RadioGroup) findViewById(R.id.rg_time_up)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int i) {
//                switch (group.getCheckedRadioButtonId()) {
//                    case R.id.rb_up_none:
//                        modifyWatchInfo.setTime_up(ClockDialData.ABOVE_TIMES.get(0).getValue());
//                        break;
//                    case R.id.rb_up_date:
//                        modifyWatchInfo.setTime_up(ClockDialData.ABOVE_TIMES.get(1).getValue());
//                        break;
//                    case R.id.rb_up_sleep:
//                        modifyWatchInfo.setTime_up(ClockDialData.ABOVE_TIMES.get(2).getValue());
//                        break;
//                    case R.id.rb_up_heart:
//                        modifyWatchInfo.setTime_up(ClockDialData.ABOVE_TIMES.get(3).getValue());
//                        break;
//                    case R.id.rb_up_step:
//                        modifyWatchInfo.setTime_up(ClockDialData.ABOVE_TIMES.get(4).getValue());
//                        break;
//                }
//            }
//        });
//        //时间下方
//        ((RadioGroup) findViewById(R.id.rg_time_down)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int i) {
//                switch (group.getCheckedRadioButtonId()) {
//                    case R.id.rb_down_none:
//                        modifyWatchInfo.setTime_down(ClockDialData.BELOW_TIMES.get(0).getValue());
//                        break;
//                    case R.id.rb_down_date:
//                        modifyWatchInfo.setTime_down(ClockDialData.BELOW_TIMES.get(1).getValue());
//                        break;
//                    case R.id.rb_down_sleep:
//                        modifyWatchInfo.setTime_down(ClockDialData.BELOW_TIMES.get(2).getValue());
//                        break;
//                    case R.id.rb_down_heart:
//                        modifyWatchInfo.setTime_down(ClockDialData.BELOW_TIMES.get(3).getValue());
//                        break;
//                    case R.id.rb_down_step:
//                        modifyWatchInfo.setTime_down(ClockDialData.BELOW_TIMES.get(4).getValue());
//                        break;
//                }
//            }
//        });
//
//        findViewById(R.id.btn_sure).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (BluetoothHelper.getInstance().isConnectOk()) {
//                    BluetoothHelper.getInstance().getTransfer().getModifyDial( modifyWatchInfo, 0, imageStatus, new OnCreateBinFileListener() {
//                        @Override
//                        public void onCreateBinFile(String jsonData) {
//                            watchData = jsonData;
////                            BluetoothHelper.getInstance().getTransfer().startFileTrans(watchData);
////                            BluetoothHelper.getInstance().getTransfer().sendPrepareDial();
//                        }
//                    });
//                }
//            }
//        });
//        BluetoothManager.supportAccelerateTransmit.observe(this, new Observer<Boolean>() {
//            @Override
//            public void onChanged(Boolean aBoolean) {
//                if (!TextUtils.isEmpty(watchData)) {
//                    BluetoothHelper.getInstance().getTransfer().startFileTrans(watchData);
//                }
//            }
//        });
//        BluetoothManager.progress.observe(this, new Observer<Integer>() {
//            @Override
//            public void onChanged(Integer progress) {
//                hud.show();
//                if (hud.isShowing()) {
//                    hud.setProgress(progress);
//                    hud.setLabel("Progress:" + progress);
//                }
//                if (progress >= 100) {
//                    hud.dismiss();
//                    modifyWatchInfo.setFilePath("");
//                    isChangePic = false;
//                    imageStatus = 0;
//                }
//            }
//        });
//    }
//
//    private boolean isCustomPic() {
//        File file = new File(Constant.FILEPATH.getCustomPath(Constant.customId + ""), Constant.CUSTOMFILENAME);
//        if (file.exists()) {
//            Constant.customImg = file.getAbsolutePath();//获取到指定手表的自定的表盘
//            return true;
//        } else {
//            Constant.customImg = "";
//            return false;
//        }
//    }
//
//    private void chooseImageFromGallery() {
//        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(pickPhoto, selectedImageResultCode);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == selectedImageResultCode && resultCode == RESULT_OK) {
//            Uri mUri = data.getData();//通过getData获取到Uri
//            String file = "custom_pic" + System.currentTimeMillis() + ".jpg";
//            startPhotoCrop(mUri, file);
//
////            Uri selectedImageUri = data.getData();
////            BitmapFactory.Options options = new BitmapFactory.Options();
////            InputStream stream = null;
////            try {
////                stream = getContentResolver().openInputStream(selectedImageUri);
////            } catch (FileNotFoundException e) {
////                e.printStackTrace();
////            }
////
////            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
////            options.inJustDecodeBounds = true;
////            String type = options.outMimeType;
////
////
////            selectedImageWith = options.outWidth;
////            selectedImageHeight = options.outHeight;
////            String imageInfo = "type =" + type + " , width = " + selectedImageWith + ", height = " + selectedImageHeight;
////            imageInfoTextView.setText(imageInfo);
////            dialImageView.setImageURI(selectedImageUri);
////
////            File file = new File(getFilesDir(), "temp");
////            try {
////                FileOutputStream fOut = new FileOutputStream(file);
////                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
////                fOut.flush();
////                fOut.close();
////                isChangePic = true;
////                radioImg = rg_img.getCheckedRadioButtonId();
////                modifyWatchInfo.setFilePath(file.getPath());
////            } catch (Exception e) {
////                Log.e("KK", e.toString());
////            }
//
//        } else if (requestCode == REQUEST_CROP  && data != null) {
//            Uri uri = data.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI);
//            dialImageView.setImageURI(uri);
//            isChangePic = true;
//            radioImg = rg_img.getCheckedRadioButtonId();
//            modifyWatchInfo.setFilePath(uri.getPath());
//            imageStatus = 2;
//        } else {
//            if (radioImg == firstRadio) {
//                rb_defaultimg.setChecked(true);
//            }
//        }
//    }
//
//    public void startPhotoCrop(Uri uri, String fileOutput) {
//
//        File dir = new File(getFilesDir().getAbsolutePath());
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        UCrop.Options options = new UCrop.Options();
//        //裁剪后图片保存在文件夹中
//        Uri destinationUri = Uri.fromFile(new File(dir, fileOutput));
//        UCrop uCrop = UCrop.of(uri, destinationUri);//第一个参数是裁剪前的uri,第二个参数是裁剪后的uri
//        //设置裁剪框的宽高比例
//        int width = BluetoothHelper.getInstance().getPersistent().getScreenWidth();
//        int high = BluetoothHelper.getInstance().getPersistent().getScreenHigh();
//        if (width <= 0 || high <= 0) {
//            uCrop.withAspectRatio(240, 240);//设置裁剪框的宽高比例
//        } else {
//            uCrop.withAspectRatio(width, high);//设置裁剪框的宽高比例
//        }
//        //下面参数分别是缩放,旋转,裁剪框的比例
//        options.setAllowedGestures(UCropActivity.ALL, UCropActivity.NONE, UCropActivity.ALL);
//        options.setToolbarTitle("提示");//设置标题栏文字
//        options.setCropGridStrokeWidth(2);//设置裁剪网格线的宽度(我这网格设置不显示，所以没效果)
//        options.setCropFrameStrokeWidth(2);//设置裁剪框的宽度
//        options.setMaxScaleMultiplier(3);//设置最大缩放比例
//        options.setHideBottomControls(true);//隐藏下边控制栏
//        options.setShowCropGrid(false);  //设置是否显示裁剪网格
//        options.setMaxBitmapSize(800);
////        options.setOvalDimmedLayer(true);//设置是否为圆形裁剪框
//        options.setShowCropFrame(true); //设置是否显示裁剪边框(true为方形边框)
//        options.setToolbarWidgetColor(Color.parseColor("#ffffff"));//标题字的颜色以及按钮颜色
//        options.setDimmedLayerColor(Color.parseColor("#AA000000"));//设置裁剪外颜色
//        options.setToolbarColor(Color.parseColor("#000000")); // 设置标题栏颜色
//        options.setStatusBarColor(Color.parseColor("#000000"));//设置状态栏颜色
//        options.setCropGridColor(Color.YELLOW);//设置裁剪网格的颜色
//        options.setCropFrameColor(Color.YELLOW);//设置裁剪框的颜色
//        uCrop.withOptions(options);
//        uCrop.start(this);
//    }
//}
