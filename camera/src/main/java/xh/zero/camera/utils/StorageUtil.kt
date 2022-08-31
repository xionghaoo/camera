package xh.zero.camera.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import java.io.File

class StorageUtil {
    companion object {
        fun hasExternalStoragePermission(context: Context): Boolean {
            val perm = context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE")
            return perm == PackageManager.PERMISSION_GRANTED
        }

        fun getDownloadDirectory(appContext: Context, dir: String) : File {
            return if (hasExternalStoragePermission(appContext)) {
                val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), dir)
                if (!downloadDir.exists()) {
                    downloadDir.mkdir()
                }
                downloadDir
            } else {
                appContext.filesDir
            }
        }
    }
}