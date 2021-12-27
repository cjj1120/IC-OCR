package com.kuhan.textrecognition

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kuhan.textrecognition.utils.getCameraIntent

class StaticTextRecognitionViewModel : ViewModel() {

    val imageUri = MutableLiveData<Uri?>().apply { value = null }
    private var cameraUri: Uri? = null

    fun updateUri(uri: Uri? = cameraUri) {
        imageUri.value = uri
    }

    fun prepareCameraIntent(): Intent? {
        val pair = getCameraIntent()
        cameraUri = pair?.second
        return pair?.first
    }
}