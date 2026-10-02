package com.example.alertchat

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import org.json.JSONObject

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        listenToSocketEvents()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "alert_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alert Chat Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Alert Chat is Running")
            .setContentText("Listening for incoming alerts...")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .build()

        startForeground(1, notification)
    }

    private fun listenToSocketEvents() {
        SocketManager.socket?.on("receive_alert") { args ->
            val data = args[0] as JSONObject
            val sender = data.getString("senderName")
            val msg = data.getString("message")

            mainLooper.run {
                showTopBanner(sender, msg)
            }
        }
    }

    // Phase 1: Call-Style Top Persistent Banner
    private fun showTopBanner(sender: String, msg: String) {
        removeCurrentOverlay()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(40, 60, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Incoming Alert from $sender"
            setTextColor(Color.WHITE)
            textSize = 18f
        }
        val preview = TextView(this).apply {
            text = msg
            setTextColor(Color.LTGRAY)
            textSize = 14f
        }

        val btnLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 20, 0, 0)
        }

        val btnOpen = Button(this).apply {
            text = "Open Chat"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                showExpandedChat(sender)
            }
        }

        val btnBusy = Button(this).apply {
            text = "Can't Attend"
            setBackgroundColor(Color.parseColor("#DC2626"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                SocketManager.sendCantAttend()
                removeCurrentOverlay()
            }
        }

        btnLayout.addView(btnOpen)
        btnLayout.addView(btnBusy)
        root.addView(title)
        root.addView(preview)
        root.addView(btnLayout)

        overlayView = root
        windowManager.addView(root, params)
    }

    // Phase 2: Active Chat Sheet (Bottom/Center)
    private fun showExpandedChat(peerName: String) {
        removeCurrentOverlay()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            1100, // Fixed height for chat card
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(30, 30, 30, 30)
        }

        // Header with Maximize & Close
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val headerText = TextView(this).apply {
            text = "Chat with $peerName"
            setTextColor(Color.WHITE)
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnClose = Button(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                showSaveConsentDialog()
            }
        }
        header.addView(headerText)
        header.addView(btnClose)
        root.addView(header)

        // Chat stream area
        val chatBox = TextView(this).apply {
            text = "--- Session Started ---\n"
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        root.addView(chatBox)

        // Input & Send
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val input = EditText(this).apply {
            hint = "Type a reply..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnSend = Button(this).apply {
            text = "Send"
            setOnClickListener {
                val txt = input.text.toString()
                if (txt.isNotBlank()) {
                    chatBox.append("You: $txt\n")
                    SocketManager.sendMessage("User", txt)
                    input.setText("")
                }
            }
        }

        bottomBar.addView(input)
        bottomBar.addView(btnSend)
        root.addView(bottomBar)

        // Listen for new messages
        SocketManager.socket?.on("receive_message") { args ->
            val data = args[0] as JSONObject
            val text = data.getString("text")
            val sender = data.getString("senderId")
            mainLooper.run {
                chatBox.append("$sender: $text\n")
            }
        }

        overlayView = root
        windowManager.addView(root, params)
    }

    // Phase 3: Dual-Consent Save Dialog
    private fun showSaveConsentDialog() {
        removeCurrentOverlay()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#334155"))
            setPadding(40, 40, 40, 40)
        }

        val msg = TextView(this).apply {
            text = "Do you want to save this conversation?"
            setTextColor(Color.WHITE)
            textSize = 16f
        }

        val btnYes = Button(this).apply {
            text = "Yes, Save"
            setOnClickListener {
                SocketManager.requestSave()
                Toast.makeText(this@OverlayService, "Save request sent to peer...", Toast.LENGTH_SHORT).show()
                removeCurrentOverlay()
            }
        }

        val btnNo = Button(this).apply {
            text = "No, Discard"
            setOnClickListener {
                SocketManager.sendSaveDecision(false)
                Toast.makeText(this@OverlayService, "Chat discarded permanently", Toast.LENGTH_SHORT).show()
                removeCurrentOverlay()
            }
        }

        root.addView(msg)
        root.addView(btnYes)
        root.addView(btnNo)

        overlayView = root
        windowManager.addView(root, params)
    }

    private fun removeCurrentOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeCurrentOverlay()
    }
}
