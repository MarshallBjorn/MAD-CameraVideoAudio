package com.example.pumsprawozdanie

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.pumsprawozdanie.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentPhotoUri: Uri? = null
    private var currentVideoUri: Uri? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String = ""
    private var isRecordingAudio = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // Initialize ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up click listeners using binding
        binding.btnTakePhoto.setOnClickListener {
            if (checkPermissions()) takePhoto()
        }

        binding.btnRecordVideo.setOnClickListener {
            if (checkPermissions()) recordVideo()
        }

        binding.btnStartAudio.setOnClickListener {
            if (checkPermissions()) startAudioRecording()
        }

        binding.btnStopAudio.setOnClickListener {
            stopAudioRecording()
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoUri != null) {
            binding.imageView.visibility = android.view.View.VISIBLE
            binding.videoView.visibility = android.view.View.GONE
            binding.imageView.setImageURI(currentPhotoUri)

            val msg = "Zapisano zdjęcie:\n$currentPhotoUri"
            binding.tvInfo.text = msg
            Toast.makeText(this, "Zapisano w: Android/data/com.example.multimedia/files/Pictures", Toast.LENGTH_LONG).show()
        }
    }

    private fun takePhoto() {
        try {
            val photoFile = createImageFile()
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            takePhotoLauncher.launch(currentPhotoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Błąd kamery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private val takeVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success && currentVideoUri != null) {
            binding.imageView.visibility = android.view.View.GONE
            binding.videoView.visibility = android.view.View.VISIBLE
            binding.videoView.setVideoURI(currentVideoUri)
            binding.videoView.start()

            val msg = "Zapisano wideo:\n$currentVideoUri"
            binding.tvInfo.text = msg
            Toast.makeText(this, "Zapisano w: Android/data/com.example.multimedia/files/Movies", Toast.LENGTH_LONG).show()
        }
    }

    private fun recordVideo() {
        try {
            val videoFile = createVideoFile()
            currentVideoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                videoFile
            )
            takeVideoLauncher.launch(currentVideoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Błąd wideo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createVideoFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile("VIDEO_${timeStamp}_", ".mp4", storageDir)
    }

    private fun startAudioRecording() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val audioFile = File.createTempFile("AUDIO_${timeStamp}_", ".3gp", storageDir)
        audioFilePath = audioFile.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                isRecordingAudio = true
                updateAudioUI(true)
                binding.tvInfo.text = "Nagrywanie audio..."
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAudioRecording() {
        if (isRecordingAudio) {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecordingAudio = false
            updateAudioUI(false)

            val msg = "Zapisano audio:\n$audioFilePath"
            binding.tvInfo.text = msg
            Toast.makeText(this, "Nagranie zapisane!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAudioUI(isRecording: Boolean) {
        binding.btnStartAudio.isEnabled = !isRecording
        binding.btnStopAudio.isEnabled = isRecording
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        permissions.add(Manifest.permission.CAMERA)
        permissions.add(Manifest.permission.RECORD_AUDIO)

        // Android 13+ (API 33) uses different permissions for media
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val listPermissionsNeeded = ArrayList<String>()
        for (p in permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 100)
            return false
        }
        return true
    }
}