package com.example.alertchat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permission check & Request
        checkOverlayPermission()

        // Connect Socket Server
        SocketManager.connect()

        // Start Background Overlay Listening Service
        val serviceIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Simple Test UI
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 80, 50, 50)
        }

        val title = android.widget.TextView(this).apply {
            text = "Alert Chat Controller"
            textSize = 22f
            setPadding(0, 0, 0, 40)
        }

        val msgInput = EditText(this).apply {
            hint = "Enter alert message to send..."
        }

        val btnTrigger = Button(this).apply {
            text = "Send Alert to Other Phone"
            setOnClickListener {
                val txt = msgInput.text.toString()
                if (txt.isNotBlank()) {
                    SocketManager.sendAlert("User 1", txt)
                    Toast.makeText(this@MainActivity, "Alert sent to peer!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(title)
        layout.addView(msgInput)
        layout.addView(btnTrigger)

        setContentView(layout)
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please allow 'Display over other apps'", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }
}
