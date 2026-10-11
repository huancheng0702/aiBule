@file:Suppress("DEPRECATION")
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import io.dcloud.uts.UTSAndroid

@Suppress("DEPRECATION")
class CoreFilePickerFragment : Fragment() {
  @Deprecated("Deprecated in Java")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    CoreFilePickerNative.handleResult(requestCode, resultCode, data)
    try {
      fragmentManager?.beginTransaction()?.remove(this)?.commitAllowingStateLoss()
      fragmentManager?.executePendingTransactions()
    } catch (e: Exception) {}
  }
}

object CoreFilePickerNative {
  private const val FRAGMENT_TAG_PREFIX = "picker_"
  private val singleCallbacks = mutableMapOf<Int, (String?) -> Unit>()
  private val multiCallbacks = mutableMapOf<Int, (String?) -> Unit>()
  private var reqCounter = 20000
  private val mainHandler = Handler(Looper.getMainLooper())

  private fun clearPickerFragments(activity: Activity) {
    try {
      val manager = activity.fragmentManager
      val fragments = manager.fragments ?: return
      val transaction = manager.beginTransaction()
      var hasRemoved = false
      for (fragment in fragments) {
        if (fragment != null && fragment.tag != null && fragment.tag!!.startsWith(FRAGMENT_TAG_PREFIX)) {
          transaction.remove(fragment)
          hasRemoved = true
        }
      }
      if (hasRemoved) {
        transaction.commitAllowingStateLoss()
        manager.executePendingTransactions()
      }
    } catch (e: Exception) {}
  }

  fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (singleCallbacks.containsKey(requestCode)) {
      val callback = singleCallbacks.remove(requestCode)
      if (resultCode == Activity.RESULT_OK && data != null) {
        callback?.invoke(data.data?.toString())
      } else {
        callback?.invoke(null)
      }
    } else if (multiCallbacks.containsKey(requestCode)) {
      val callback = multiCallbacks.remove(requestCode)
      if (resultCode == Activity.RESULT_OK && data != null) {
        val uris = mutableSetOf<String>()
        val clip = data.clipData
        if (clip != null) {
          for (i in 0 until clip.itemCount) {
            val u = clip.getItemAt(i).uri
            if (u != null) uris.add(u.toString())
          }
        } else {
          val single = data.data
          if (single != null) uris.add(single.toString())
        }
        if (uris.isNotEmpty()) {
          val arr = org.json.JSONArray()
          uris.forEach { arr.put(it) }
          callback?.invoke(arr.toString())
        } else {
          callback?.invoke(null)
        }
      } else {
        callback?.invoke(null)
      }
    }
  }

  fun getFileInfo(context: Context, uriStr: String): Map<String, Any?>? {
    return try {
      val uri = Uri.parse(uriStr)
      val cursor = context.contentResolver.query(uri, null, null, null, null)
      cursor?.use { c ->
        if (c.moveToFirst()) {
          val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
          val fileName = if (nameIndex >= 0) c.getString(nameIndex) else "unknown"
          val fileSize = if (sizeIndex >= 0) c.getLong(sizeIndex) else 0L
          val mimeType = context.contentResolver.getType(uri) ?: ""
          mapOf("fileName" to (fileName ?: "unknown"), "fileSize" to fileSize, "mimeType" to mimeType)
        } else null
      }
    } catch (e: Exception) { null }
  }

  fun processUriToCache(context: Context, uriStr: String, destName: String?): String? {
    return try {
      val uri = Uri.parse(uriStr)
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val mimeType = context.contentResolver.getType(uri) ?: "*/*"
      val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "tmp"
      val name = destName ?: "file_${System.currentTimeMillis()}.$ext"
      val baseDir = context.externalCacheDir ?: context.cacheDir
      val destFile = java.io.File(baseDir, name)
      inputStream.use { it.copyTo(destFile.outputStream()) }
      "file://" + destFile.absolutePath
    } catch (e: Exception) { null }
  }

  fun startSinglePick(mimeTypes: List<String>?, callback: (uri: String?) -> Unit) {
    val activity = UTSAndroid.getUniActivity()
    if (activity == null) { callback(null); return }
    val code = reqCounter++
    singleCallbacks[code] = callback

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "*/*"
      if (!mimeTypes.isNullOrEmpty() && mimeTypes[0] != "*/*") {
        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
      }
    }

    mainHandler.post {
      try {
        val frag = CoreFilePickerFragment()
        activity.fragmentManager.beginTransaction()
          .add(frag, FRAGMENT_TAG_PREFIX + code)
          .commitAllowingStateLoss()
        activity.fragmentManager.executePendingTransactions()
        frag.startActivityForResult(intent, code)
      } catch (e: Exception) {
        singleCallbacks.remove(code)
        callback(null)
      }
    }
  }

  fun startMultiPick(mimeTypes: List<String>?, callback: (jsonResult: String?) -> Unit) {
    val activity = UTSAndroid.getUniActivity()
    if (activity == null) { callback(null); return }
    val code = reqCounter++
    multiCallbacks[code] = callback

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "*/*"
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      if (!mimeTypes.isNullOrEmpty() && mimeTypes[0] != "*/*") {
        putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
      }
    }

    mainHandler.post {
      try {
        val frag = CoreFilePickerFragment()
        activity.fragmentManager.beginTransaction()
          .add(frag, FRAGMENT_TAG_PREFIX + code)
          .commitAllowingStateLoss()
        activity.fragmentManager.executePendingTransactions()
        frag.startActivityForResult(intent, code)
      } catch (e: Exception) {
        multiCallbacks.remove(code)
        callback(null)
      }
    }
  }
}
