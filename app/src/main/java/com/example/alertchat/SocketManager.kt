package com.example.alertchat

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {
    private const val SERVER_URL = "https://alert-chat-backend.onrender.com"
    var socket: Socket? = null

    fun connect() {
        if (socket == null) {
            try {
                socket = IO.socket(SERVER_URL)
                socket?.connect()
                // Default test room join kar rahe hain
                socket?.emit("join_room", "global_test_room")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendAlert(sender: String, msg: String) {
        val json = JSONObject()
        json.put("roomId", "global_test_room")
        json.put("senderName", sender)
        json.put("message", msg)
        json.put("timestamp", System.currentTimeMillis())
        socket?.emit("send_alert", json)
    }

    fun sendMessage(sender: String, msg: String) {
        val json = JSONObject()
        json.put("roomId", "global_test_room")
        json.put("senderId", sender)
        json.put("text", msg)
        json.put("timestamp", System.currentTimeMillis())
        socket?.emit("send_message", json)
    }

    fun sendCantAttend() {
        val json = JSONObject()
        json.put("roomId", "global_test_room")
        socket?.emit("cant_attend", json)
    }

    fun requestSave() {
        val json = JSONObject()
        json.put("roomId", "global_test_room")
        socket?.emit("request_save", json)
    }

    fun sendSaveDecision(agreed: Boolean) {
        val json = JSONObject()
        json.put("roomId", "global_test_room")
        json.put("agreed", agreed)
        socket?.emit("save_consent_response", json)
    }
}
