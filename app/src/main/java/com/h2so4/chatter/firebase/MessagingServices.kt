package com.h2so4.chatter.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.h2so4.chatter.R
import com.h2so4.chatter.models.NotificationApiService
import com.h2so4.chatter.models.NotificationRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }
    private fun showNotification(title: String?, body: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel("chat_notifications")
            if (existingChannel == null) {
                val channel = NotificationChannel("chat_notifications", "Chat Notifications", NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
        }

        val notificationBuilder = NotificationCompat.Builder(this, "chat_notifications")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.c_logo)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToServer(token)
    }
    private fun sendTokenToServer(token: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.134:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(NotificationApiService::class.java)
        val notificationRequest = NotificationRequest(token, "Token Registration", "Your FCM token has been registered.")
        apiService.sendNotification(notificationRequest).enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e("FCM", "Error sending token", t)
            }
            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Log.d("FCM", "Token sent successfully")
                } else {
                    Log.e("FCM", "Failed to send token: ${response.code()}")
                }
            }
        })
    }
}