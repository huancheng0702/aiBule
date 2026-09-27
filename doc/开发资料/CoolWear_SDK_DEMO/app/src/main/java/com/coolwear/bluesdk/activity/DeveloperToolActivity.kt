//package com.coolwear.bluesdk.activity
//
//import android.os.Bundle
//import android.text.InputType
//import android.text.TextUtils
//import android.util.Log
//import android.view.MenuItem
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.Observer
//import ce.com.cenewbluesdk.entity.k6.K6_Action
//import ce.com.cenewbluesdk.proxy.sdkhelper.BluetoothHelper
//import com.coolwear.bluesdk.BluetoothManager
//import com.coolwear.bluesdk.R
//import com.coolwear.bluesdk.utils.FileUtils
//import com.kaopiz.kprogresshud.KProgressHUD
//import com.liulishuo.okdownload.DownloadListener
//import com.liulishuo.okdownload.DownloadTask
//import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
//import com.liulishuo.okdownload.core.cause.EndCause
//import com.liulishuo.okdownload.core.cause.ResumeFailedCause
//import com.obsez.android.lib.filechooser.ChooserDialog
//import java.io.File
//
//class DeveloperToolActivity : AppCompatActivity(), DownloadListener {
//
//    private var hud: KProgressHUD? = null
//    private var dialSendCompleteDialog: AlertDialog? = null
//
//    private var dialFilePath: String? = null
//    private var filePath: String ?=null
//    override fun onBackPressed() {
//        super.onBackPressed()
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == android.R.id.home) {
//            finish()
//        }
//        return super.onOptionsItemSelected(item)
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_developer)
//
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//
//        initUI()
//
//        BluetoothManager.progress.observe(this, Observer { progress ->
//            Log.i("KK", "transfer dial file progress $progress")
//            hud?.isShowing?.let {
//                hud?.setProgress(progress)
//                hud?.setLabel("Progress: $progress%")
//            }
//            if (progress >= 100) {
//                BluetoothManager.progress.value=0;
//                hud?.dismiss()
//                if (dialSendCompleteDialog == null) {
//                    dialSendCompleteDialog = AlertDialog.Builder(this)
//                            .setTitle("Success")
//                            .setPositiveButton("OK") { _, _ -> }
//                            .create()
//                    dialSendCompleteDialog?.show()
//                } else {
//                    dialSendCompleteDialog?.let {
//                        if (!it?.isShowing!!) {
//                            it?.show()
//                        }
//                    }
//                }
//
//            }
//        })
//        BluetoothManager.connectStatus.observe(this, Observer {
//            if (it != K6_Action.RCVD.BLUE_CONNECTED) {
//                hud?.dismiss()
//            }
//        })
//    }
//
//    private var tvFilePath: TextView? = null
//
//    private fun initUI() {
//        hud = KProgressHUD(this)
//            .setStyle(KProgressHUD.Style.BAR_DETERMINATE)
//            .setLabel("Please wait")
//            .setMaxProgress(100)
//
//        val editText = findViewById<EditText>(R.id.editText_url)
//
//        // choose dial file from internet download
//        val btnDialConfirm = findViewById<Button>(R.id.btn_dial_confirm)
//        btnDialConfirm.setOnClickListener {
//            hud?.setLabel("Please wait")?.setProgress(0)
//            hud?.show()
//
//            downloadDialFile(url = editText.text.toString())
//        }
//
//        // choose dial file from local import
//        val btnSelectDialFile = findViewById<Button>(R.id.btn_select_dial_file)
//        btnSelectDialFile.setOnClickListener {
//            chooseFile()
//        }
//        val btnDialConfirm2 = findViewById<Button>(R.id.btn_dial_confirm_2)
//        btnDialConfirm2.setOnClickListener {
//            if (dialFilePath == null) return@setOnClickListener
//
//            val tEditText = EditText(this)
//            tEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
//            AlertDialog.Builder(this)
//                .setTitle("Check file ID")
//                .setView(tEditText)
//                .setPositiveButton("OK") { _, _ ->
//                    try {
//                        val file = File(dialFilePath!!)
//                        if (tEditText.text.toString().trim()
//                                .equals(FileUtils.checkDialFileId(file), true)
//                        ) {
//                            hud?.setLabel("Please wait")?.setProgress(0)
//                            hud?.show()
//                            filePath = file.path
//                            BluetoothHelper.getInstance().transfer.sendPrepareDial()
//                        } else {
//                            Toast.makeText(this, "Check bin file error", Toast.LENGTH_SHORT).show()
//                        }
//                    } catch (e: java.lang.Exception) {
//                        hud?.dismiss()
//                        Toast.makeText(this, "读取文件出错", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                .setCancelable(true)
//                .show()
//        }
//        tvFilePath = findViewById(R.id.tv_dial_file)
//
//        BluetoothManager.supportAccelerateTransmit.observe(
//                this,
//                Observer { supportAccelerateTransmit ->
//                    // If support accelerate transmit set the isSpeedUp to true.
////                BleFactory.getInstance().k6Proxy.isSpeedUp = supportAccelerateTransmit
////                BleFactory.getInstance().k6Proxy.startFileTrans(fileJson)
//
//                    if (!TextUtils.isEmpty(filePath)) {
//                        BluetoothHelper.getInstance().transfer.startMoreFileTrans(filePath)
////                    BleFactory.getInstance().k6Proxy.startFileTrans(filePath);
//                    }
//                })
//    }
//
//    private fun chooseFile() {
//
//        ChooserDialog(this@DeveloperToolActivity)
////            .withFilter(false, null)context.getExternalFilesDir
//            .withStartFile(getExternalFilesDir("WATCH_BIN_FILE")?.path)
//            .withChosenListener { path: String, pathFile: File ->
//                val fileName = path.substring(path.lastIndexOf("/") + 1)
//                dialFilePath = path
//                tvFilePath?.text = fileName
//                FileUtils.checkDialFileId(pathFile)
//            }
//            .withOnCancelListener { dialog ->
//                Log.d("CANCEL", "CANCEL")
//                dialog.cancel() // MUST have
//            }
//            .build()
//            .show()
//    }
//
//    private fun downloadDialFile(url: String) {
//        // path = cacheDir + "/download_dial.bin"
//        val downloadTask =
//            DownloadTask.Builder(url, cacheDir).setFilename("download_dial.bin").build()
//        downloadTask.enqueue(this)
//    }
//
//    override fun connectTrialEnd(
//            task: DownloadTask,
//            responseCode: Int,
//            responseHeaderFields: MutableMap<String, MutableList<String>>
//    ) {
//        Log.i("KK", "DownloadTask listen")
//    }
//
//    override fun fetchEnd(task: DownloadTask, blockIndex: Int, contentLength: Long) {
//        Log.i("KK", "DownloadTask listen")
//    }
//
//    override fun downloadFromBeginning(
//            task: DownloadTask,
//            info: BreakpointInfo,
//            cause: ResumeFailedCause
//    ) {
//        val percent = info.totalOffset.toFloat() / info.totalLength * 100
//        Log.i("KK", "current percent = $percent")
//    }
//
//    override fun taskStart(task: DownloadTask) {
//        Log.i("KK", "DownloadTask taskStart")
//    }
//
//    override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?) {
//        if (cause == EndCause.COMPLETED) {
//            Log.i("KK", "Download success")
//            task.file?.let {
//                filePath = it.path;
////                syncDialFile(it)
//                BluetoothHelper.getInstance().transfer.sendPrepareDial()
//            }
//        } else {
//            hud?.dismiss()
//            Log.i("KK", "Download Failed")
//            Toast.makeText(applicationContext, "网络错误或链接出错", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    override fun connectTrialStart(
//            task: DownloadTask,
//            requestHeaderFields: MutableMap<String, MutableList<String>>
//    ) {
//        Log.i("KK", "DownloadTask listen")
//    }
//
//    override fun downloadFromBreakpoint(task: DownloadTask, info: BreakpointInfo) {
//        Log.i("KK", "DownloadTask listen")
//    }
//
//    override fun fetchStart(task: DownloadTask, blockIndex: Int, contentLength: Long) {
//        Log.i("KK", "DownloadTask listen")
//    }
//
//    override fun fetchProgress(task: DownloadTask, blockIndex: Int, increaseBytes: Long) {
//        Log.i("KK", "DownloadTask listen")
//    }
//
//    override fun connectEnd(
//            task: DownloadTask,
//            blockIndex: Int,
//            responseCode: Int,
//            responseHeaderFields: MutableMap<String, MutableList<String>>
//    ) {
//        Log.i("KK", "DownloadTask listen")
//    }
//
//    override fun connectStart(
//            task: DownloadTask,
//            blockIndex: Int,
//            requestHeaderFields: MutableMap<String, MutableList<String>>
//    ) {
//        Log.i("KK", "DownloadTask listen")
//    }
//
//}