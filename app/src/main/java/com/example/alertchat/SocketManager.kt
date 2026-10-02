package com.example.alertchat

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object SocketManager {
    private const val SERVER_URL = "https://alert-chat-backend.onrender.com/"
    var socket: Socket? = null

    var currentRoomId: String = "1234"
    var currentUserName: String = "User"
    var connectedPeerName: String? = null

    fun init() {
        if (socket == null) {
            try {
                socket = IO.socket(SERVER_URL)
                socket?.connect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun joinRoom(roomId: String, userName: String) {
        currentRoomId = roomId
        currentUserName = userName
        val data = JSONObject().apply {
            put("roomId", roomId)
            put("userName", userName)
        }
        socket?.emit("join_room", data)
    }

    fun sendAlert(msg: String) {
        val data = JSONObject().apply {
            put("roomId", currentRoomId)
            put("senderName", currentUserName)
            put("message", msg)
        }
        socket?.emit("send_alert", data)
    }

    fun sendMessage(msg: String) {
        val data = JSONObject().apply {
            put("roomId", currentRoomId)
            put("senderName", currentUserName)
            put("text", msg)
        }
        socket?.emit("send_message", data)
    }

    fun sendCantAttend() {
        val data = JSONObject().apply {
            put("roomId", currentRoomId)
            put("senderName", currentUserName)
            put("text", "I'm busy right now, cannot attend!")
        }
        socket?.emit("send_message", data)
    }

    fun requestSave() {}
    fun sendSaveDecision(decision: Boolean) {}
}
