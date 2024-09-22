package com.h2so4.chatter.activities

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FirebaseFirestore
import com.h2so4.chatter.BuildConfig
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.ChatAdapter
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivityChatBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Callback
import java.io.IOException

class ChatActivity: BaseActivity() {

    private lateinit var ui: ActivityChatBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: FirebaseDatabase
    private lateinit var size: DisplayMetrics
    private lateinit var adapter: ChatAdapter
    private val messages = ArrayList<Message>()
    private var chatReference: DatabaseReference? = null
    private var sender: Chatter? = null
    private var receiver: Chatter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityChatBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        size = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(size)
        ui.messagesList.translationY = size.heightPixels.toFloat() * -1
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DB_URL)
        sender = intent.getParcelableExtra("sender")
        receiver = intent.getParcelableExtra("receiver")
        setInfo()
        setChatAdapter()
        ui.send.setOnClickListener { send() }
    }

    private fun setInfo() {
        ui.chatterInfo.chatterUsername.text = receiver?.username
        if (receiver?.profilePicture != null && receiver?.profilePicture!!.isNotBlank()) ui.chatterInfo.chatterPictureIn.setImageBitmap(
            ChattersAdapter.decodeImage(receiver?.profilePicture)
        )
        else {
            when (receiver?.gender) {
                "Male" -> ui.chatterInfo.chatterPictureIn.setImageResource(R.drawable.male_user_icon)
                else -> ui.chatterInfo.chatterPictureIn.setImageResource(R.drawable.female_user_icon)
            }
            ui.chatterInfo.chatterPictureIn.setColorFilter(
                ContextCompat.getColor(
                    this,
                    R.color.seriousYellow
                )
            )
        }
        ui.profile.setOnClickListener {
            val profileIntent = Intent(this, ProfileActivity::class.java)
            profileIntent.putExtra("visitor", sender)
            profileIntent.putExtra("account", receiver)
            profileIntent.putExtra("comeBack", true)
            startActivity(profileIntent)
        }
    }
    private fun setChatAdapter() {
        ui.loadingChat.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val reference1 = database.getReference("Chats").child("${sender?.username}|${receiver?.username}")
            val check1 = reference1.get().await()
            val reference2 = database.getReference("Chats").child("${receiver?.username}|${sender?.username}")
            val check2 = reference2.get().await()
            val info = firestore.collection("Chatters").document(receiver?.username!!).get().await()
            receiver?.token = info.getString("FCMToken")
            listenToAvailable()
            val get = if(check1.exists()) check1 else if(check2.exists()) check2 else null
            chatReference = get?.ref
            if (get != null) {
                for (i in get.children) messages.add(i.getValue(Message::class.java)!!)
            } else withContext(Dispatchers.Main) { ui.sayHello.visibility = View.VISIBLE }
            withContext(Dispatchers.Main) {
                adapter = ChatAdapter(sender!!, receiver!!, messages, this@ChatActivity)
                ui.messagesList.adapter = adapter
                ui.messagesList.layoutManager = LinearLayoutManager(this@ChatActivity)
                ui.messagesList.setHasFixedSize(true)
                ui.messagesList.scrollToPosition(adapter.itemCount - 1)
                ui.messagesList.animate().apply {
                    duration = 1000
                    translationY(0f)
                }.withEndAction {
                    ui.loadingChat.visibility = View.INVISIBLE
                    setKeyboardListener()
                    setupChatListener()
                }.start()
            }
        }
    }
    private fun send() {
        fun fly() {
            ui.inputMessage.text = null
            ui.send.isClickable = false
            ui.send.animate().apply {
                duration = 250
                translationX(size.widthPixels.toFloat() / 2f)
            }.withEndAction {
                ui.send.translationX = size.widthPixels.toFloat() * -1
                ui.send.animate().apply {
                    duration = 250
                    translationX(0f)
                }.withEndAction { ui.send.isClickable = true }.start()
            }.start()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            if (ui.inputMessage.text.toString().isNotBlank()) {
                val newMessage = Message(sender?.username!!, ui.inputMessage.text.toString(), ServerValue.TIMESTAMP)
                if(chatReference != null) chatReference!!.push().setValue(newMessage)
                else {
                    withContext(Dispatchers.Main) { ui.send.isClickable = false }
                    firestore.collection("Chatters").document(sender?.username!!)
                        .collection("ChatChatters").document(receiver?.username!!).set(emptyMap<String, Any>()).await()
                    firestore.collection("Chatters").document(receiver?.username!!)
                        .collection("ChatChatters").document(sender?.username!!).set(emptyMap<String, Any>()).await()
                    database.getReference("Chats").child("${sender?.username}|${receiver?.username}")
                        .push().setValue(newMessage).await()
                    chatReference = database.getReference("Chats").child("${sender?.username}|${receiver?.username}")
                    withContext(Dispatchers.Main) {
                        ui.loadingChat.visibility = View.INVISIBLE
                        ui.sayHello.visibility = View.INVISIBLE
                        ui.send.isClickable = true
                    }
                    setupChatListener()
                }
                withContext(Dispatchers.Main) {
                    if(ui.chatterInfo.chatterLastMessage.text.toString() != "Online")
                        sendNotificationToServer(receiver?.token ?: "", sender?.username!!, newMessage.message)
                    fly()
                }

            }
        }
    }
    private fun setupChatListener() {
        chatReference?.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val newMessage = snapshot.getValue(Message::class.java)!!
                if(!adapter.messages.contains(newMessage)) {
                    adapter.addMessage(newMessage)
                    ui.messagesList.scrollToPosition(adapter.itemCount - 1)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error:  DatabaseError) {}
        })
    }
    private fun listenToAvailable() {
        firestore.collection("Chatters").document(receiver?.username!!).addSnapshotListener { value, error ->
            if(error != null) return@addSnapshotListener
            if(value != null) {
                try {
                    ui.chatterInfo.chatterLastMessage.text =
                        ContextCompat.getString(this, R.string.last_seen)
                            .plus(ChatAdapter.setLocalTime(value.getTimestamp( "Available")?.toDate()?.time!!))
                } catch(e: Exception) { ui.chatterInfo.chatterLastMessage. text = ContextCompat.getString(this, R.string.online) }
                receiver?.token = value.getString("FCMToken")
            }
        }
    }
    private fun setKeyboardListener() {
        ui.inputMessage.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(100)
                ui.messagesList.scrollToPosition(adapter.itemCount - 1)
            }
        }
        ui.inputMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(50)
                    ui.messagesList.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }
    }
    private fun sendNotificationToServer(token: String, title: String, message: String) {
        val client = OkHttpClient()
        val json = """
        {
            "token": "$token",
            "title": "$title",
            "message": "$message"
        }
    """.trimIndent()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url(BuildConfig.CHATTER_SERVER_URL)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                lifecycleScope.launch(Dispatchers.Main) { Toast.makeText(this@ChatActivity, "+++FFF", Toast.LENGTH_SHORT).show() }
                e.printStackTrace()
            }
            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    lifecycleScope.launch(Dispatchers.Main) { Toast.makeText(this@ChatActivity, "AAA", Toast.LENGTH_SHORT).show() }
                    println("Notification sent successfully")
                } else {
                    lifecycleScope.launch(Dispatchers.Main) { Toast.makeText(this@ChatActivity, "FFF", Toast.LENGTH_SHORT).show() }
                    println("Error sending notification: ${response.message}")
                }
            }
        })
    }
}