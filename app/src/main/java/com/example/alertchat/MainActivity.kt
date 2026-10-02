package com.example.alertchat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Overlay Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        SocketManager.init()

        // Background Foreground Service Start
        val serviceIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Programmatic UI
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 80, 50, 50)
            setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
        }

        val statusText = TextView(this).apply {
            text = "🔴 Status: Waiting for peer to connect..."
            setTextColor(android.graphics.Color.YELLOW)
            textSize = 16f
            setPadding(0, 0, 0, 40)
        }

        val nameInput = EditText(this).apply {
            hint = "Enter Your Name (e.g. Realme 1)"
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.WHITE)
        }

        val roomInput = EditText(this).apply {
            hint = "Pairing Code (e.g. 7842)"
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.WHITE)
            setText("7842")
        }

        val btnConnect = Button(this).apply {
            text = "Join & Pair Room"
            setBackgroundColor(android.graphics.Color.parseColor("#2563EB"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                val name = nameInput.text.toString().trim()
                val room = roomInput.text.toString().trim()
                if (name.isNotEmpty() && room.isNotEmpty()) {
                    SocketManager.joinRoom(room, name)
                    Toast.makeText(this@MainActivity, "Connected to Room $room as $name", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Please enter Name & Code", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val alertMsgInput = EditText(this).apply {
            hint = "Urgent message..."
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 40, 0, 20)
        }

        val btnSendAlert = Button(this).apply {
            text = "🚨 Send Pop-up Alert"
            setBackgroundColor(android.graphics.Color.parseColor("#DC2626"))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                val msg = alertMsgInput.text.toString().ifEmpty { "Call me urgently!" }
                SocketManager.sendAlert(msg)
                Toast.makeText(this@MainActivity, "Alert Sent!", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(statusText)
        layout.addView(nameInput)
        layout.addView(roomInput)
        layout.addView(btnConnect)
        layout.addView(alertMsgInput)
        layout.addView(btnSendAlert)

        setContentView(layout)

        // Socket Events for Connection Presence
        SocketManager.socket?.on("peer_connected") { args ->
            val data = args[0] as JSONObject
            val peer = data.optString("peerName", "Friend")
            SocketManager.connectedPeerName = peer
            mainHandler.post {
                statusText.text = "🟢 Connected with: $peer"
                statusText.setTextColor(android.graphics.Color.GREEN)
                Toast.makeText(this@MainActivity, "$peer joined!", Toast.LENGTH_LONG).show()
            }
        }

        SocketManager.socket?.on("peer_disconnected") { args ->
            val data = args[0] as JSONObject
            val peer = data.optString("peerName", "Peer")
            mainHandler.post {
                statusText.text = "🔴 $peer disconnected"
                statusText.setTextColor(android.graphics.Color.RED)
            }
        }
    }
}
