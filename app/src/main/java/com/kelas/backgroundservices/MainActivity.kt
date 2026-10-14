package com.kelas.backgroundservices

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kelas.backgroundservices.R

class MainActivity : ComponentActivity() {

    private lateinit var btnMasuk: Button
    private lateinit var statusTextView: TextView
    private lateinit var jumpCountTextView: TextView
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("InlinedApi")
    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    } else {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        btnMasuk = findViewById(R.id.btnMasuk)
        statusTextView = findViewById(R.id.statusTextView)
        jumpCountTextView = findViewById(R.id.jumpCount)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                startTrackerService()
            } else {
                statusTextView.text = "Izin ditolak, service tidak dapat dijalankan"
            }
        }

        btnMasuk.setOnClickListener {
            if (hasAllPermissions()) {
                startTrackerService()
            } else {
                requestPermissionLauncher.launch(permissionsToRequest)
            }
        }

        jumpingTracker()

        handleIntent(intent)
    }

    private fun hasAllPermissions(): Boolean {
        return permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startTrackerService() {
        val serviceIntent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        statusTextView.text = "Background service sedang berjalan"
    }

    private fun jumpingTracker(){
        val serviceIntent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        jumpCountTextView.text = "Jumps: " + jumps
    }

    @SuppressLint("SetTextI18n")
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SERVICE_STOPPED_JUMP) {
            statusTextView.text = "Background services sudah berhenti karena Anda melompat"
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    companion object {
        @Suppress("unused")
        const val ACTION_SERVICE_STOPPED_JUMP = "com.kelas.BackgroundService.ACTION_SERVICE_STOPPED_JUMP"
    }
}
