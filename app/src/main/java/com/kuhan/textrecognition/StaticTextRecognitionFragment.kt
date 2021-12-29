package com.kuhan.textrecognition

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kuhan.textrecognition.databinding.FragmentStaticTextRecognitionBinding
import com.kuhan.textrecognition.utils.*
import java.util.*
import kotlin.math.log

class StaticTextRecognitionFragment : Fragment() {

    private val activityForPermission = ActivityResultContracts.RequestPermission()
    private val activityForResult = ActivityResultContracts.StartActivityForResult()
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissionListener: (Boolean) -> Unit
    private lateinit var storagePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var storagePermissionListener: (Boolean) -> Unit
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    private lateinit var binding: FragmentStaticTextRecognitionBinding
    private val viewModel by viewModels<StaticTextRecognitionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraPermissionLauncher = registerForActivityResult(activityForPermission) {
            cameraPermissionListener(it)
        }

        storagePermissionLauncher = registerForActivityResult(activityForPermission) {
            storagePermissionListener(it)
        }

        cameraLauncher = registerForActivityResult(activityForResult) {
            if (it.resultCode == Activity.RESULT_OK) viewModel.updateUri()
        }

        galleryLauncher = registerForActivityResult(activityForResult) {
            viewModel.updateUri(it.data?.data ?: return@registerForActivityResult)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {

        binding = FragmentStaticTextRecognitionBinding.inflate(inflater, container, false)

        binding.btnSaveOutput.isVisible = false

        binding.btnTakePhoto.setOnClickListener {
            val intent = viewModel.prepareCameraIntent()
            intent ?: return@setOnClickListener showToast("Unable to prep camera")
            if (checkPermission(CAMERA)) cameraLauncher.launch(intent)
            else {
                cameraPermissionLauncher.launch(CAMERA)
                cameraPermissionListener = {
                    if (it) cameraLauncher.launch(intent)
                    else showToast("Permission not granted by the user.")
                }
            }
        }

        binding.btnSelectGallery.setOnClickListener {
            galleryLauncher.launch(prepareGalleryIntent())
        }

        viewModel.imageUri.observe(viewLifecycleOwner) {
            binding.boOverlay.clear()
            binding.ivPreview.setImageURI(it)
            processImage(it)
        }

        return binding.root
    }

    private fun processImage(uri: Uri?) {
        val image = uri ?: return showToast("Invalid URI")
        binding.btnSaveOutput.isVisible = false
        try {
            val inputImage = InputImage.fromFilePath(requireActivity(), image)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(inputImage)
                .addOnSuccessListener {
                    binding.btnSaveOutput.isVisible = true
                    binding.boOverlay.add(it, inputImage.width, inputImage.height)
                    binding.btnSaveOutput.setOnClickListener { _ ->
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                            if (checkPermission(WRITE_EXTERNAL_STORAGE)) saveOutput(it)
                            else {
                                cameraPermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
                                cameraPermissionListener = { isAllowed ->
                                    if (isAllowed) saveOutput(it)
                                    else showToast("Permission not granted by the user.")
                                }
                            }
                        } else saveOutput(it)
                    }
                }
                .addOnFailureListener {
                    binding.btnSaveOutput.isVisible = false
                    showToast(it.localizedMessage)
                }
        } catch (ex: Exception) {
            showToast(ex.localizedMessage)
        }
    }

    private fun saveOutput(text: Text) {
        val textViewOCR: TextView = binding.textViewOCR

        val OCRtext = MLKitUtils().getTextFile(text)
        Log.e("FINAL", OCRtext[0])
        Log.e("FINAL", OCRtext[1])
        binding.textViewOCR.text = OCRtext[0]
        binding.textViewOCR2.text = OCRtext[1]



        val builder = NotificationCompat.Builder(App.context, "0000")
            .setSmallIcon(R.drawable.ic_done)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

//        if (file != null && saveFileToDownloads(file)) {
//            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
//            val pendingIntent = PendingIntent.getActivity(App.context, 0, intent, 0)
//            builder.setContentTitle("Downloaded")
//            builder.setContentText("ML Kit text has been saved to downloads.")
//            builder.setContentIntent(pendingIntent)
//        } else {
//            builder.setContentTitle("Failed")
//            builder.setContentText("ML Kit text has failed to export output.")
//        }
        NotificationManagerCompat.from(App.context).notify(0, builder.build())
    }
}
