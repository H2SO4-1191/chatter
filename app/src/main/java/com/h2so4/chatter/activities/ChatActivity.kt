package com.h2so4.chatter.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.h2so4.chatter.BuildConfig
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.ChatAdapter
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivityChatBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Message
import com.h2so4.chatter.models.PreRegex
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
import org.json.JSONObject
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
    private var inChat: Int? = 0

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
        sender?.profilePicture = PreRegex.me
        receiver?.profilePicture = PreRegex.them
        ui.send.isEnabled = false
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
            ui.profile.isClickable = false
            val profileIntent = Intent(this, ProfileActivity::class.java)
            PreRegex.me = sender?.profilePicture?:""
            sender?.profilePicture = null
            PreRegex.them = receiver?.profilePicture?:""
            receiver?.profilePicture = null
            profileIntent.putExtra("visitor", sender)
            profileIntent.putExtra("account", receiver)
            profileIntent.putExtra("comeBack", true)
            startActivity(profileIntent)
            sender?.profilePicture = PreRegex.me
            receiver?.profilePicture = PreRegex.them
            lifecycleScope.launch {
                delay(1000)
                ui.profile.isClickable = true
            }
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
                for (i in get.children)
                    if(i.hasChildren()) {
                        if(i.child("sender").getValue(String::class.java) == receiver?.username) chatReference?.child(i.key!!)?.child("state")?.setValue(1)
                        messages.add(i.getValue(Message::class.java)!!)
                    }
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
                    setInChatListener()
                    ui.send.isEnabled = true
                }.start()
            }
        }
    }
    private fun send() {
        fun fly() {
            ui.inputMessage.text = null
            ui.send.isEnabled = false
            ui.send.animate().apply {
                duration = 250
                translationX(size.widthPixels.toFloat() / 2f)
            }.withEndAction {
                ui.send.translationX = size.widthPixels.toFloat() * -1
                ui.send.animate().apply {
                    duration = 250
                    translationX(0f)
                }.withEndAction { ui.send.isEnabled = true }.start()
            }.start()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            if (ui.inputMessage.text.toString().isNotBlank()) {
                val inChatChat = if(inChat == 0 || inChat == null) 0 else 1
                val newMessage = Message(sender?.username!!, ui.inputMessage.text.toString(), inChatChat ,ServerValue.TIMESTAMP)
                if(chatReference != null) {
                    chatReference!!.push().setValue(newMessage)
                    withContext(Dispatchers.Main) { fly() }
                } else {
                    withContext(Dispatchers.Main) {
                        ui.inputMessage.text = null
                        ui.send.isEnabled = false
                        ui.send.animate().apply {
                            duration = 600
                            rotation(360f)
                        }.start()
                        delay(300)
                        ui.creatingChat.visibility = View.VISIBLE
                        ui.send.visibility = View.INVISIBLE
                    }
                    firestore.collection("Chatters").document(sender?.username!!)
                        .collection("ChatChatters").document(receiver?.username!!).set(emptyMap<String, Any>()).await()
                    firestore.collection("Chatters").document(receiver?.username!!)
                        .collection("ChatChatters").document(sender?.username!!).set(emptyMap<String, Any>()).await()
                    database.getReference("Chats").child("${sender?.username}|${receiver?.username}")
                        .push().setValue(newMessage).await()
                    database.getReference("Chats").child("${sender?.username}|${receiver?.username}")
                        .child(sender?.username!!).setValue(1).await()
                    database.getReference("Chats").child("${sender?.username}|${receiver?.username}")
                        .child(receiver?.username!!).setValue(0).await()
                    chatReference = database.getReference("Chats").child("${sender?.username}|${receiver?.username}")
                    withContext(Dispatchers.Main) {
                        ui.send.animate().apply {
                            duration = 600
                            rotation(720f)
                        }.start()
                        delay(250)
                        ui.creatingChat.visibility = View.INVISIBLE
                        ui.send.visibility = View.VISIBLE
                        ui.sayHello.visibility = View.INVISIBLE
                        ui.send.isEnabled = true
                    }
                    setupChatListener()
                    setInChatListener()
                }
                withContext(Dispatchers.Main) { if(inChat == 0) sendNotificationToServer(receiver?.token ?: "", sender?.username!!, newMessage.message) }
            }
            firestore.collection("Chatters")
                .document(sender?.username!!)
                .collection("ChatChatters")
                .document(receiver?.username!!)
                .set(hashMapOf("Last" to FieldValue.serverTimestamp()))
            firestore.collection("Chatters")
                .document(receiver?.username!!)
                .collection("ChatChatters")
                .document(sender?.username!!)
                .set(hashMapOf("Last" to FieldValue.serverTimestamp()))
        }
    }
    private fun setupChatListener() {
        chatReference?.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if(snapshot.hasChildren()) {
                    val newMessage = snapshot.getValue(Message::class.java)!!
                    var exists = false
                    for(i in adapter.messages) if(i.toString() == newMessage.toString()) {
                        exists = true
                        break
                    }
                    if(!exists) {
                        adapter.addMessage(newMessage)
                        adapter.notifyItemInserted(adapter.itemCount - 1)
                        ui.messagesList.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error:  DatabaseError) {}
        })
        ui.inputMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if(ui.inputMessage.text.toString().isNotBlank()) chatReference?.child(sender?.username!!)?.setValue(2)
                else chatReference?.child(sender?.username!!)?.setValue(1)
            }
        })
    }
    private fun listenToAvailable() {
        firestore.collection("Chatters").document(receiver?.username!!).addSnapshotListener { value, error ->
            if(error != null) return@addSnapshotListener
            if(value != null) {
                try {
                    ui.chatterInfo.chatterLastMessage.text =
                        ContextCompat.getString(this, R.string.last_seen)
                            .plus(" ").plus(ChatAdapter.setLocalTime(value.getTimestamp( "Available")?.toDate()?.time!!))
                } catch(e: Exception) {
                    if(value.getLong("Available") == 1L) ui.chatterInfo.chatterLastMessage. text = ContextCompat.getString(this, R.string.online)
                }
                receiver?.token = value.getString("FCMToken")
            }
        }
    }
    private fun setInChatListener() {
        chatReference?.child(sender?.username!!)?.setValue(1)
        chatReference?.child(receiver?.username!!)?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) inChat = snapshot.getValue(Int::class.java)
                if(inChat == 1) {
                    for (i in 0..<adapter.messages.size) {
                        adapter.messages[i].state = 1
                        adapter.notifyItemChanged(i)
                    }
                }
                if(inChat == 2) ui.chatterInfo.chatterLastMessage.text = ContextCompat.getString(this@ChatActivity, R.string.typing)
                else if(inChat == 1) ui.chatterInfo.chatterLastMessage.text = ContextCompat.getString(this@ChatActivity, R.string.online)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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
        val json = JSONObject().apply {
            put("token", token)
            put("title", title)
            put("message", message)
        }
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url(BuildConfig.CHATTER_SERVER_URL)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) println("Notification sent successfully")
                else println("Error sending notification: ${response.message}")
            }
        })
    }
    override fun onPause() {
        super.onPause()
        chatReference?.child(sender?.username!!)?.setValue(0)
    }
    override fun onResume() {
        super.onResume()
        chatReference?.child(sender?.username!!)?.setValue(1)
    }
    override fun onDestroy() {
        super.onDestroy()
        chatReference?.child(sender?.username!!)?.setValue(0)
    }
}