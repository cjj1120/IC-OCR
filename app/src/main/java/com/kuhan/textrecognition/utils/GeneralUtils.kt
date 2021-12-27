package com.kuhan.textrecognition.utils

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.kuhan.textrecognition.App
import com.kuhan.textrecognition.BuildConfig
import com.kuhan.textrecognition.R
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

fun AppCompatActivity.openFragment(fragment: Fragment) = with(this) {
    this.supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, fragment)
        .addToBackStack(null)
        .commit()
}

fun showToast(message: String?, length: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(App.context, message ?: "Error", length).show()

fun Activity.checkPermissions(permissions: Array<String>): Boolean = permissions.all {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

fun Fragment.checkPermission(permission: String): Boolean =
    activity?.checkPermissions(arrayOf(permission)) ?: false

fun prepareGalleryIntent(): Intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }

fun getCameraIntent(): Pair<Intent, Uri>? {

    // Create camera intent
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    // Ensure that there's a camera activity to handle the intent
    App.context.packageManager.let { intent.resolveActivity(it) ?: return null }

    // Create the File where the photo should go
    val file: File = createImageFile() ?: return null
    val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
    val photoURI: Uri = FileProvider.getUriForFile(App.context, authority, file)
    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

    return Pair(intent, photoURI)
}

fun createImageFile(): File? {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = ContextCompat.getExternalFilesDirs(App.context, Environment.DIRECTORY_PICTURES)
    return try {
        val dir = if (storageDir.isNotEmpty()) storageDir[0] else null
        File.createTempFile("JPEG_${timeStamp}_", ".jpg", dir)
    } catch (ex: Throwable) {
        null
    }
}

fun saveFileToDownloads(file: File): Boolean {
    return try {
        val resolver = App.context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, file.extension)
                put(MediaStore.MediaColumns.SIZE, file.length())
            }
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            val dlDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
            val destinyFile = File(dlDir, file.name)
            FileProvider.getUriForFile(App.context, authority, destinyFile)
        }?.also { downloadedUri ->
            resolver.openOutputStream(downloadedUri).use { outputStream ->
                val brr = ByteArray(1024)
                var len: Int
                val bufferedInputStream = BufferedInputStream(FileInputStream(file.absoluteFile))
                while ((bufferedInputStream.read(brr, 0, brr.size).also { len = it }) != -1) {
                    outputStream?.write(brr, 0, len)
                }
                outputStream?.flush()
                bufferedInputStream.close()
            }
        }
        true
    } catch (t: Throwable) {
        false
    }
}

fun createNotificationChannel() {
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel("0000", "Updates", importance)
    val manager = App.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}