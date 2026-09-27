package com.coolwear.bluesdk.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.InputStream

object FileUtils {

    /**
     * Get dial file id. Take the 5th and 6th byte of the dial file.
     * When you transfer dial file to device, should check the file's id first.
     * @param file the dial file
     * @return dial file id
     */
    fun checkDialFileId(file: File): String? {
        val fileBytes = file.readBytes()
        if (fileBytes.size > 6) {
            return String.format("%02x", fileBytes[4])  + String.format("%02x", fileBytes[5])
        }
        return null
    }

    /**
     * Save inputStream to current application's file folder
     *
     * @param context Context
     * @param uri source file uri
     * @param fileName destination file name
     * @return destination file path
     */
    fun getPathFromInputStreamUri(context: Context, uri: Uri, fileName: String): String? {
        var inputStream: InputStream? = null
        var filePath: String? = null

        if (uri.authority != null) {
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                val file = inputStream?.let { createTemporalFileFrom(context, it, fileName) }
                filePath = file?.path
            } catch (e: Exception) {
                Log.e("KK", e.toString())
            } finally {
                try {
                    inputStream?.close()
                } catch (e: Exception) {
                    Log.e("KK", e.toString())
                }
            }
        }

        return filePath
    }

    private fun createTemporalFileFrom(
            context: Context,
            inputStream: InputStream,
            fileName: String
    ): File? {
        val buffer = inputStream.readBytes()
        val targetFile: File?
        try {
            targetFile = when {
                fileName.endsWith("bin") -> {
                    File(context.getExternalFilesDir("WATCH_BIN_FILE"), fileName)
                }
                fileName.endsWith("img") -> {
                    File(context.getExternalFilesDir("IMG_FILE"), fileName)
                }
                else -> {
                    File(context.getExternalFilesDir("OtherFile"), fileName)
                }
            }
            if (targetFile.exists()) {
                targetFile.delete()
            }
            targetFile.writeBytes(buffer)
        } catch (e: Exception) {
            throw e
        }
        return targetFile
    }

}