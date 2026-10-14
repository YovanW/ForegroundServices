package com.kelas.backgroundservices

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.kelas.backgroundservices.R
import java.util.Calendar
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    private lateinit var btnMasuk: Button
    private lateinit var btnTambahLompatan: Button
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
        btnTambahLompatan = findViewById(R.id.btnTambahLompatan)
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

        btnTambahLompatan.setOnClickListener {
            jumpCountTextView.text = "Jumps: " + jumps
            jumps+=1
            jumpingTracker()
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

        if (jumps >= 3){
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("asia/jakarta"))
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                    val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->

                        val selectedDateTime = Calendar.getInstance().apply {
                            set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
                        }

                        val endTime = (selectedDateTime.clone() as Calendar).apply {
                            add(Calendar.HOUR_OF_DAY, 1)
                        }

                        val eventIntent = Intent(Intent.ACTION_INSERT).apply {
                            data = CalendarContract.Events.CONTENT_URI
                            putExtra(CalendarContract.Events.TITLE, "Jumping")
                            putExtra(CalendarContract.Events.DESCRIPTION,
                                "Jumlah Lompatan anda: $jumps"
                            )
                            putExtra(CalendarContract.Events.ALL_DAY, false)
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, selectedDateTime.timeInMillis)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.timeInMillis)
                        }
                        if (eventIntent.resolveActivity(packageManager) != null) {
                            startActivity(eventIntent)
                        } else {
                            Toast.makeText(this, "Tidak ada aplikasi Kalender ditemukan", Toast.LENGTH_SHORT).show()
                        }

                    }, hour, minute, true)
                    timePickerDialog.show()
                }, year, month, day)
                datePickerDialog.show()

        }
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
