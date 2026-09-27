package com.coolwear.bluesdk.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.coolwear.bluesdk.R
import com.coolwear.bluesdk.utils.FileUtils
import java.io.File

class ImportFileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_file)

        val tvImportTips = findViewById<TextView>(R.id.tv_import_tips)

        val btnImportConfirm = findViewById<Button>(R.id.btn_import_confirm)
        btnImportConfirm.setOnClickListener {
            finish()
        }

        if (intent.action == Intent.ACTION_VIEW) {
//            val hasPermission = AndPermission.hasPermissions(
//                this,
//                arrayOf(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)
//            )
            val uri = intent.data
            if (uri != null) {
                val filePath = FileUtils.getPathFromInputStreamUri(this, uri, File("" + uri).name)
                if (filePath != null) {
                    tvImportTips.text = "Import file success!"
                } else {
                    tvImportTips.text = "Import file Failed!"
                }
            }
        }
    }

}