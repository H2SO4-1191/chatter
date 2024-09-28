package com.h2so4.chatter.firebase

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.h2so4.chatter.R
import com.h2so4.chatter.activities.LaunchActivity
import com.h2so4.chatter.adapters.ChattersAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MessagingService: FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"]
            val body = remoteMessage.data["body"]
            showNotification(title, body)
        }
    }
    private fun showNotification(title: String?, body: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            val doc = FirebaseFirestore.getInstance().collection("Chatters").document(title!!).get().await()
            val avatar = doc.getString("ProfilePicture")
            val avatarBitmap = if (avatar != null) ChattersAdapter.decodeImage(avatar)
            else Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            val circularAvatar = getCircularBitmap(avatarBitmap)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val existingChannel = notificationManager.getNotificationChannel("chat_notifications")
                if (existingChannel == null) {
                    val channel = NotificationChannel("chat_notifications", "Chat Notifications", NotificationManager.IMPORTANCE_HIGH)
                    notificationManager.createNotificationChannel(channel)
                }
            }
            val intent = Intent(this@MessagingService, LaunchActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(
                this@MessagingService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notificationBuilder = NotificationCompat.Builder(this@MessagingService, "chat_notifications")
                .setSmallIcon(R.drawable.c_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setLargeIcon(circularAvatar)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
        }
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        val rect = Rect(left, top, left + size, top + size)
        val rectF = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, Rect(0, 0, size, size), paint)
        return output
    }
}