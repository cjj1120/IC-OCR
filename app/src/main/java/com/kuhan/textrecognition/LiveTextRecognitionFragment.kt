package com.kuhan.textrecognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kuhan.textrecognition.databinding.FragmentLiveTextRecognitionBinding
import com.kuhan.textrecognition.utils.checkPermission
import com.kuhan.textrecognition.utils.showToast
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveTextRecognitionFragment : Fragment() {

    private lateinit var binding: FragmentLiveTextRecognitionBinding

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request Camera Permission
        if (checkPermission(Manifest.permission.CAMERA)) startCamera()
        else {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) startCamera()
                else showToast("Permission not granted by the user.")
            }.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLiveTextRecognitionBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.preview.surfaceProvider)
            }

            imageAnalyzer = ImageAnalysis.Builder().build().apply {
                setAnalyzer(cameraExecutor) { processImage(it) }
            }

            // Select back camera
            val cs = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cs, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("LiveTextRecognitionFragment", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(ip: ImageProxy) {
        val image = InputImage.fromMediaImage(ip.image ?: return, ip.imageInfo.rotationDegrees)




        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
            .addOnSuccessListener {

                when(activity?.resources?.configuration?.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> binding.boOverlay.add(it, ip.width, ip.height)
                    Configuration.ORIENTATION_PORTRAIT -> binding.boOverlay.add(it, ip.height, ip.width)
                    else -> binding.boOverlay.add(it, ip.height, ip.width)
                }

                ip.close()
            }
            .addOnFailureListener { ip.close() }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
