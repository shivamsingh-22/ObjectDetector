package com.example.objectdetectorapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : ComponentActivity() {

    private lateinit var objectImage: ImageView
    private lateinit var lableText: TextView
    private lateinit var captureImageBtn: Button
    private lateinit var imageLabeler: ImageLabeler


    private val cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Get the image as a Bitmap
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap

                if (imageBitmap != null) {
                    objectImage.setImageBitmap(imageBitmap)
                    lableText.text = "Processing..." // Give feedback to the user
                    labelImage(imageBitmap)
                } else {
                    lableText.text = "Could not get image."
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }
        }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission was granted by the user, now we can launch the camera.
                launchCamera()
            } else {
                // User denied the permission.
                Toast.makeText(this, "Permission denied. Camera cannot be used.", Toast.LENGTH_LONG).show()
                lableText.text = "Camera permission is needed to use this feature."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout)

        objectImage = findViewById(R.id.imageView)
        lableText = findViewById(R.id.resultTextView)
        captureImageBtn = findViewById(R.id.ctrBtn)

        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        captureImageBtn.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
    }

    private fun labelImage(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(inputImage)
            .addOnSuccessListener { labels ->
                displayLabel(labels)
            }
            .addOnFailureListener { e ->
                lableText.text = "Error: ${e.message}"
            }
    }

    private fun displayLabel(labels: List<ImageLabel>) {
        if (labels.isNotEmpty()) {
            val topLabel = labels[0]
            val resultText = "Object: ${topLabel.text}\nConfidence: ${"%.1f".format(topLabel.confidence * 100)}%"
            lableText.text = resultText
        } else {
            lableText.text = "No Object Recognized"
        }
    }


    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted.
                launchCamera()
            }
            else -> {
                // Permission has not been granted, so we ask for it.
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Check if there is an app that can handle the camera intent
        if (cameraIntent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

}