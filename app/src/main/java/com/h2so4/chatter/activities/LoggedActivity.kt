package com.h2so4.chatter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.ChatAdapter
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivityLoggedBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Message
import com.h2so4.chatter.models.Pop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.h2so4.chatter.BuildConfig
import com.h2so4.chatter.models.PreRegex

class LoggedActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var ui: ActivityLoggedBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var shared: SharedPreferences
    private lateinit var chattersAdapter: ChattersAdapter
    private lateinit var aiAdapter: ChatAdapter
    private lateinit var chatChatters: ArrayList<Chatter>
    private lateinit var questions: ArrayList<Message>
    private lateinit var ai :GenerativeModel
    private var size = DisplayMetrics()
    private var chatter: Chatter? = null
    private var chatChattersListener: ListenerRegistration? = null
    private var toggled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityLoggedBinding.inflate(layoutInflater)
        setContentView(ui.root)
        FirebaseApp.initializeApp(this)
        shared = getSharedPreferences("chatter", Context.MODE_PRIVATE)
        windowManager.defaultDisplay.getRealMetrics(size)
        database = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        manageOtherLayouts()
        lifecycleScope.launch(Dispatchers.IO) { signedIn() }
    }

    private fun signedIn() {
        lifecycleScope.launch(Dispatchers.IO) {
            if(intent.getBooleanExtra("logged", false)) {
                chatter = Chatter(
                    fullName = shared.getString("fullName", null),
                    username = shared.getString("username", null),
                    email = shared.getString("email", null),
                    phoneNumber = shared.getString("phoneNumber", null),
                    birth = shared.getString("birth", null),
                    gender = shared.getString("gender", null),
                    profilePicture = null,
                    password = null,
                    token = null
                )
                PreRegex.me = shared.getString("profilePicture", "")?:""
            } else {
                chatter = intent.getParcelableExtra("chatter")
                chatter?.profilePicture = PreRegex.me
                withContext(Dispatchers.Main) { hint("Hello, ${chatter?.fullName}.") }
            }
            if(auth.currentUser?.isEmailVerified == true) {
                withContext(Dispatchers.Main) {
                    pass()
                    getToken()
                }
            } else verification()
        }
    }
    private fun pass() {
        ui.verificationLayoutInclude.verificationLayout.visibility = View.GONE
        setDrawer()
        setChatterHeaderInfo()
        setAddChatters()
        setAiAdapter()
        lifecycleScope.launch(Dispatchers.Main) { getToken() }
    }
    private fun setChattersAdapter() {
        ui.chatChattersInclude.chattersList.translationX = size.widthPixels.toFloat()*1.25f
        lifecycleScope.launch(Dispatchers.IO) {
            chatChatters = ArrayList()
            val chattersUsernames = ArrayList<String>()
            withContext(Dispatchers.Main) { load(true) }
            val search: QuerySnapshot? = try { database.collection("Chatters").document(chatter?.username!!).collection("ChatChatters").orderBy("Last", Query.Direction.DESCENDING).get().await() }
            catch(e: Exception) { null }
            if(search?.isEmpty == false) {
                withContext(Dispatchers.Main) { ui.chatChattersInclude.noChatters.visibility = View.INVISIBLE }
                for (i in search) {
                    val contact = database.collection("Chatters").document(i.id).get().await()
                    if(!chattersUsernames.contains(contact.getString("Username"))) {
                        chattersUsernames.add(contact.getString("Username")?:"")
                        chatChatters.add(
                            Chatter(
                                username = contact.getString("Username"),
                                profilePicture = contact.getString("ProfilePicture"),
                                gender = contact.getString("Gender"),
                                fullName = null, email = null, phoneNumber = null, password = null, birth = null, token = null
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    chattersAdapter = ChattersAdapter(this@LoggedActivity, chatter!!, chatChatters, 1) { receiver ->
                        if(ui.chatChattersInclude.chattersList.isEnabled) {
                            ui.chatChattersInclude.chattersList.isEnabled = false
                            val chatIntent = Intent(this@LoggedActivity, ChatActivity::class.java)
                            PreRegex.me = chatter?.profilePicture?:""
                            PreRegex.them = receiver.profilePicture?:""
                            chatter?.profilePicture = null
                            receiver.profilePicture = null
                            chatIntent.putExtra("sender", chatter)
                            chatIntent.putExtra("receiver", receiver)
                            startActivity(chatIntent)
                            chatter?.profilePicture = PreRegex.me
                            receiver.profilePicture = PreRegex.them
                            lifecycleScope.launch {
                                delay(1000)
                                ui.chatChattersInclude.chattersList.isEnabled = true
                            }
                        }
                    }
                    ui.chatChattersInclude.chattersList.adapter = chattersAdapter
                    ui.chatChattersInclude.chattersList.layoutManager = LinearLayoutManager(this@LoggedActivity)
                    ui.chatChattersInclude.chattersList.setHasFixedSize(true)
                    ui.chatChattersInclude.chattersList.animate().apply {
                        duration = 600
                        translationX(0f)
                    }.start()
                    load(false)
                    listenToChatChatters()
                }
            } else {
                withContext(Dispatchers.Main) {
                    ui.chatChattersInclude.noChatters.visibility = View.VISIBLE
                    load(false)
                }
            }
        }
    }
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun listenToChatChatters() {
        chatChattersListener = database.collection("Chatters")
            .document(chatter?.username!!)
            .collection("ChatChatters").addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                if (value != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val currentChatters = ArrayList<String>()
                        for(i in chattersAdapter.chatters) if(i.username != null) currentChatters.add(i.username!!)
                        for(i in value.documentChanges) {
                            when(i.type){
                                DocumentChange.Type.ADDED -> {
                                    if(!currentChatters.contains(i.document.id) && i.document.id.isNotBlank()) {
                                        val newChatter = database.collection("Chatters").document(i.document.id).get().await()
                                        var exists = false
                                        for(j in  chattersAdapter.chatters) {
                                            if(j.username == newChatter.getString("Username")) {
                                                exists = true
                                                break
                                            }
                                        }
                                        if(!exists) {
                                            chattersAdapter.chatters.add(0, Chatter(
                                                username = newChatter.getString("Username"),
                                                profilePicture = newChatter.getString("ProfilePicture"),
                                                gender = newChatter.getString("Gender"),
                                                fullName = null, email = null, phoneNumber = null, password = null, birth = null, token = null
                                            ))
                                        }
                                        withContext(Dispatchers.Main) {
                                            delay(500)
                                            chattersAdapter.notifyItemInserted(0)
                                        }
                                    }
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    if(currentChatters.contains(i.document.id)) {
                                        var chatterToMove: Chatter? = null
                                        for(j in chattersAdapter.chatters) {
                                            if(j.username == i.document.id) {
                                                chatterToMove = j
                                                break
                                            }
                                        }
                                        if(chatterToMove != null) {
                                            withContext(Dispatchers.Main) {
                                                val oldIndex = chattersAdapter.chatters.indexOf(chatterToMove)
                                                chattersAdapter.chatters.removeAt(oldIndex)
                                                chattersAdapter.chatters.add(0, chatterToMove)
                                                chattersAdapter.notifyItemMoved(oldIndex, 0)
                                                delay(100)
                                                val cell = ui.chatChattersInclude.chattersList.findViewHolderForLayoutPosition(0)
                                                val text = cell?.itemView?.findViewById<TextView>(R.id.chatterLastMessage)
                                                when(text?.text.toString()) {
                                                    "" -> {
                                                        text?.setTextColor(ContextCompat.getColor(this@LoggedActivity, R.color.white))
                                                        text?.text = "1 New message"
                                                    }
                                                    "No new messages" -> {
                                                        text?.setTextColor(ContextCompat.getColor(this@LoggedActivity, R.color.white))
                                                        text?.text = "1 New message"
                                                    }
                                                    "1 new message" -> text?.text = "2 New messages"
                                                    else -> {
                                                        val string = text?.text.toString()
                                                        val num = string.subSequence(0, string.indexOf("N")).trim().toString().toInt() + 1
                                                        text?.text = "$num New messages"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
    }
    private fun setAddChatters() {
        ui.chatChattersInclude.searchChattersInclude.foundChatters.translationX = size.widthPixels.toFloat()*1.25f
        val add = ui.chatChattersInclude.searchChattersButton
        val layout = ui.chatChattersInclude.searchChattersInclude.searchChattersLayout
        val searchBar = ui.chatChattersInclude.searchChattersInclude.searchBar
        fun searchInput(input: String) {
            val foundChatters = ArrayList<Chatter>()
            ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = null
            var search: QuerySnapshot?
            if(input.isNotBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        load(true)
                        ui.chatChattersInclude.searchChattersInclude.chatterNotFound.visibility = View.INVISIBLE
                    }
                    search = database.collection("Chatters").get().await()
                    withContext(Dispatchers.Main) { load(false) }
                    for(i in search!!) {
                        if(i.id.contains(input) && i.id != chatter?.username) {
                            foundChatters.add(
                                Chatter(
                                    username = i.getString("Username"),
                                    gender = i.getString("Gender"),
                                    profilePicture = i.getString("ProfilePicture"),
                                    fullName = i.getString("FullName"),
                                    email = null, phoneNumber = null, password = null, birth = null, token = null
                                )
                            )
                        }
                    }
                    if(foundChatters.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val foundChattersAdapter = ChattersAdapter(this@LoggedActivity, chatter!!, foundChatters, 0) { account ->
                                if(ui.chatChattersInclude.searchChattersInclude.foundChatters.isEnabled) {
                                    ui.chatChattersInclude.searchChattersInclude.foundChatters.isEnabled = false
                                    val profileIntent = Intent(this@LoggedActivity, ProfileActivity::class.java)
                                    PreRegex.me = chatter?.profilePicture?:""
                                    chatter?.profilePicture = null
                                    PreRegex.them = account.profilePicture?:""
                                    account.profilePicture = null
                                    profileIntent.putExtra("visitor", chatter)
                                    profileIntent.putExtra("account", account)
                                    startActivity(profileIntent)
                                    chatter?.profilePicture = PreRegex.me
                                    account.profilePicture = PreRegex.them
                                    lifecycleScope.launch {
                                        delay(1000)
                                        ui.chatChattersInclude.searchChattersInclude.foundChatters.isEnabled = true
                                    }
                                }
                            }
                            ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = foundChattersAdapter
                            ui.chatChattersInclude.searchChattersInclude.foundChatters.layoutManager = LinearLayoutManager(this@LoggedActivity)
                            ui.chatChattersInclude.searchChattersInclude.foundChatters.setHasFixedSize(true)
                            ui.chatChattersInclude.searchChattersInclude.foundChatters.animate().apply {
                                duration = 600
                                translationX(0f)
                            }.start()
                        }
                    } else withContext(Dispatchers.Main) {
                        ui.chatChattersInclude.searchChattersInclude.chatterNotFound.visibility = View.VISIBLE
                        ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = null
                    }
                }
            } else {
                search = null
                foundChatters.clear()
                ui.chatChattersInclude.searchChattersInclude.chatterNotFound.visibility = View.INVISIBLE
                ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = null
            }
        }
        add.setOnClickListener {
            if(!toggled) {
                ProfileActivity.layoutIsEnabled(ui.chatChattersInclude.chatChattersLayout,
                    ui.chatChattersInclude.searchChattersInclude.searchChattersLayout, false)
                toggled = true
                add.isEnabled = false
                layout.visibility = View.VISIBLE
                add.animate().apply {
                    duration = 500
                    rotation(-405f)
                }.start()
                layout.animate().apply {
                    duration = 500
                    translationX(0f)
                }.withEndAction { add.isEnabled = true }
            } else {
                ProfileActivity.layoutIsEnabled(ui.chatChattersInclude.chatChattersLayout,
                    ui.chatChattersInclude.searchChattersInclude.searchChattersLayout, true)
                toggled = false
                add.isEnabled = false
                add.animate().apply {
                    duration = 500
                    rotation(0f)
                }.start()
                layout.animate().apply {
                    duration = 500
                    translationX(size.widthPixels.toFloat())
                }.withEndAction {
                    val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(ui.chatChattersInclude.searchChattersInclude.searchBar.windowToken, 0)
                    ui.chatChattersInclude.searchChattersInclude.chatterNotFound.visibility = View.INVISIBLE
                    ui.chatChattersInclude.searchChattersInclude.searchBar.text = null
                    ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = null
                    layout.visibility = View.VISIBLE
                    add.isEnabled = true
                }.start()
            }
        }
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                runnable?.let { handler.removeCallbacks(it) }
                ui.chatChattersInclude.searchChattersInclude.foundChatters.animate().apply {
                    duration = 600
                    translationX(size.widthPixels.toFloat()*-1.25f)
                }.withEndAction {
                    ui.chatChattersInclude.searchChattersInclude.foundChatters.translationX = size.widthPixels.toFloat()*1.25f
                    ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = null
                }.start()
            }
            override fun afterTextChanged(s: Editable?) {
                runnable = Runnable { searchInput(searchBar.text.toString().trim().uppercase()) }
                handler.postDelayed(runnable!!, 1500)
            }
        })
    }
    private suspend fun getToken() {
        val token = FirebaseMessaging.getInstance().token.await()
        database.collection("Chatters").document(chatter?.username!!).update("FCMToken", token).await()
    }
    private fun chatChattersSite() {
        if(ui.chatChattersInclude.searchChattersInclude.searchChattersLayout.isVisible) closeAddChatters()
        else if(ui.chatterAiInclude.chatterAiLayout.isVisible) {
            ui.chatterAiInclude.chatterAiLayout.animate().apply {
                duration = 600
                translationY(size.heightPixels.toFloat()*-1)
            }.withEndAction { ui.chatterAiInclude.chatterAiLayout.visibility = View.INVISIBLE }.start()
        }
        onBackPressed()
        ProfileActivity.layoutIsEnabled(ui.drawerLayout, null, true)
    }
    private fun chatterAccount() {
        lifecycleScope.launch(Dispatchers.Main) {
            val profileIntent = Intent(this@LoggedActivity, ProfileActivity::class.java)
            PreRegex.them = chatter?.profilePicture?:""
            chatter?.profilePicture = null
            profileIntent.putExtra("visitor", chatter)
            profileIntent.putExtra("account", chatter)
            startActivity(profileIntent)
            chatter?.profilePicture = PreRegex.them
            delay(1000)
            onBackPressed()
        }
    }
    private fun chatterAI() {
        val prompt = ui.chatterAiInclude.inputQ
        var questionPrompt = ""
        val ask = ui.chatterAiInclude.sendPrompt
        val load = ui.chatterAiInclude.answering
        val qList = ui.chatterAiInclude.qList
        prompt.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(100)
                qList.scrollToPosition(aiAdapter.itemCount - 1)
            }
        }
        prompt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(50)
                    qList.scrollToPosition(aiAdapter.itemCount - 1)
                }
            }
        }
        onBackPressed()
        ProfileActivity.layoutIsEnabled(ui.chatChattersInclude.chatChattersLayout, null, false)
        ui.chatterAiInclude.chatterAiLayout.visibility = View.VISIBLE
        ui.chatterAiInclude.chatterAiLayout.animate().apply {
            duration = 600
            translationY(0f)
        }.start()
        ask.setOnClickListener {
            if(prompt.text.toString().isNotBlank()) {
                lifecycleScope.launch(Dispatchers.Main) {
                    questionPrompt = prompt.text.toString()
                    prompt.text = null
                    ask.isClickable = false
                    ask.animate().apply {
                        duration = 600
                        rotation(360f)
                    }.withEndAction { ask.rotation = 0f }.start()
                    delay(300)
                    load.visibility = View.VISIBLE
                    ask.visibility = View.INVISIBLE
                    aiAdapter.addMessage(
                        Message(
                            chatter?.username!!,
                            questionPrompt,
                            0,
                            System.currentTimeMillis()
                        )
                    )
                    qList.scrollToPosition(aiAdapter.itemCount - 1)
                    lifecycleScope.launch(Dispatchers.IO) {
                        val response = try {
                            ai.generateContent(questionPrompt).text.toString()
                        } catch (e: Exception) {
                            "Something went wrong, please check your internet connection."
                        }
                        withContext(Dispatchers.Main) {
                            ask.animate().apply {
                                duration = 600
                                rotation(360f)
                            }.withEndAction { ask.rotation = 0f }.start()
                            delay(250)
                            load.visibility = View.INVISIBLE
                            ask.visibility = View.VISIBLE
                            ask.isClickable = true
                            val cleanedResponse = response.replace("*", "")
                            aiAdapter.addMessage(
                                Message(
                                    "ChatterAI",
                                    cleanedResponse,
                                    0,
                                    System.currentTimeMillis()
                                )
                            )
                            qList.scrollToPosition(aiAdapter.itemCount - 1)
                        }
                    }
                }
            }
        }
    }
    private fun setAiAdapter() {
        ai = GenerativeModel("gemini-pro", BuildConfig.AI_API_KEY)
        questions = ArrayList()
        aiAdapter = ChatAdapter(chatter!!,
            Chatter(profilePicture = SignupActivity.encodeImage((ContextCompat.getDrawable(this, R.drawable.c_logo) as BitmapDrawable).bitmap),
                username = "ChatterAI", fullName = null, email = null, password = null, phoneNumber = null, gender = null, birth = null, token = null),
            questions, this
        )
        ui.chatterAiInclude.qList.adapter = aiAdapter
        ui.chatterAiInclude.qList.setHasFixedSize(true)
        ui.chatterAiInclude.qList.layoutManager = LinearLayoutManager(this@LoggedActivity)
    }
    private fun about() {
        startActivity(Intent(this, AboutActivity::class.java))
    }
    private fun logOut() {
        SignupActivity.showYesNoDialog(this, ContextCompat.getString(this, R.string.logout_confirm),
            onYes = {
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        load(true)
                    }
                    database.collection("Chatters").document(chatter?.username!!).update("FCMToken", FieldValue.delete()).await()
                    auth.signOut()
                    shared.edit().clear().apply()
                    startActivity(Intent(this@LoggedActivity, MainActivity::class.java))
                    finish()
                }
            },
            onNo = {})
    }
    @SuppressLint("SetTextI18n")
    private suspend fun verification() {
        suspend fun load(state: Boolean) {
            if(state) {
                withContext(Dispatchers.Main) {
                    ui.verificationLayoutInclude.verifying.visibility = View.VISIBLE
                    ui.verificationLayoutInclude.verifyButton.visibility = View.INVISIBLE
                }
            } else {
                withContext(Dispatchers.Main) {
                    ui.verificationLayoutInclude.verifying.visibility = View.INVISIBLE
                    ui.verificationLayoutInclude.verifyButton.visibility = View.VISIBLE
                }
            }
        }
        suspend fun reload() {
            load(true)
            auth.currentUser?.reload()?.await()
            load(false)
        }
        suspend fun sendEmail() {
            load(true)
            auth.currentUser?.sendEmailVerification()?.await()
            withContext(Dispatchers.Main) { hint("Verification email had been sent to ${chatter?.email}") }
            load(false)
            withContext(Dispatchers.Main) {
                ui.verificationLayoutInclude.countdown.visibility = View.VISIBLE
                for (count in 59 downTo 0) {
                    @SuppressLint("SetTextI18n")
                    ui.verificationLayoutInclude.countdown.text = "00:$count"
                    delay(1000)
                }
                ui.verificationLayoutInclude.countdown.visibility = View.INVISIBLE
            }
        }
        withContext(Dispatchers.Main) {
            val verifyLayoutBinding = ui.verificationLayoutInclude
            val verifyView = ui.verificationLayoutInclude.verificationLayout
            verifyLayoutBinding.askToVerify.text = "${ContextCompat.getString(this@LoggedActivity, R.string._verifyEmail)} \"${chatter?.email}\" ${ContextCompat.getString(this@LoggedActivity, R.string.verifyEmail_)}"
            verifyView.visibility = View.VISIBLE
            verifyView.animate().apply {
                duration = 1000
                translationY(0f)
            }.start()
            verifyLayoutBinding.verifyButton.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    reload()
                    if(auth.currentUser?.isEmailVerified == true) {
                        withContext(Dispatchers.Main) {
                            verifyLayoutBinding.verifyButton.visibility = View.INVISIBLE
                            verifyLayoutBinding.done.visibility = View.VISIBLE
                            verifyView.animate().apply {
                                duration = 2000
                                translationY(size.heightPixels.toFloat())
                            }.withEndAction {
                                verifyView.visibility = View.GONE
                                pass()
                            }.start()
                        }
                    } else {
                        if(ui.verificationLayoutInclude.countdown.visibility == View.INVISIBLE) {
                            withContext(Dispatchers.Main) {
                                try { sendEmail() }
                                catch(e: Exception) { hint("A verification email had just been sent to ${chatter?.email}") }
                                load(false)
                                ui.verificationLayoutInclude.countdown.visibility = View.VISIBLE
                                for(countDown in 59 downTo  0) {
                                    ui.verificationLayoutInclude.countdown.text = "00:$countDown"
                                    delay(1000)
                                }
                                ui.verificationLayoutInclude.countdown.visibility = View.INVISIBLE
                            }
                        }
                        else withContext(Dispatchers.Main) { hint("Email had already been sent, verify or wait for the countdown to get one again.") }
                    }
                }
            }
            verifyLayoutBinding.signOutVerification.setOnClickListener { logOut() }
        }
    }
    private fun setChatterHeaderInfo() {
        val drawerInfo = ui.navigationDrawer.getHeaderView(0)
        drawerInfo.findViewById<TextView>(R.id.userNameHeader).text = chatter?.username
        drawerInfo.findViewById<TextView>(R.id.fullNameHeader).text = chatter?.fullName
        val profilePicture = drawerInfo.findViewById<ImageView>(R.id.profilePictureHeader)
        if(!chatter?.profilePicture.isNullOrBlank()) profilePicture.setImageBitmap(ChattersAdapter.decodeImage(chatter?.profilePicture!!))
        else {
            when(chatter?.gender){
                "Male" -> profilePicture.setImageResource(R.drawable.male_user_icon)
                else -> profilePicture.setImageResource(R.drawable.female_user_icon)
            }
            profilePicture.background = null
        }
    }
    private fun setDrawer() {
        drawerLayout = ui.drawerLayout
        navigationView = ui.navigationDrawer
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navigationView.bringToFront()
        navigationView.setNavigationItemSelectedListener { menuItem ->
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) { menuItem.isEnabled = false }
                when(menuItem.title) {
                    "Chatter Account" -> withContext(Dispatchers.Main) { chatterAccount() }
                    "Chat Chatters" -> withContext(Dispatchers.Main) { chatChattersSite() }
                    "Chatter AI" -> withContext(Dispatchers.Main) { chatterAI() }
                    "Log-Out" -> withContext(Dispatchers.Main) { logOut() }
                    "About" -> withContext(Dispatchers.Main) { about() }
                    else -> withContext(Dispatchers.Main) { hint("Feature not implemented.") }
                }
                delay(1000)
                withContext(Dispatchers.Main) { menuItem.isEnabled = true }
            }
            false
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        ProfileActivity.layoutIsEnabled(ui.drawerLayout, null, true)
        if(drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
        else if(ui.chatterAiInclude.chatterAiLayout.isVisible) {
            ui.chatterAiInclude.chatterAiLayout.animate().apply {
                duration = 600
                translationY(size.heightPixels.toFloat()*-1)
            }.withEndAction { ui.chatterAiInclude.chatterAiLayout.visibility = View.INVISIBLE }.start()
        } else if(ui.chatChattersInclude.searchChattersInclude.searchChattersLayout.isVisible) closeAddChatters()
        else super.onBackPressed()
    }
    private fun closeAddChatters() {
        toggled = false
        ui.chatChattersInclude.searchChattersButton.isEnabled = false
        ui.chatChattersInclude.searchChattersButton.animate().apply {
            duration = 500
            rotation(0f)
        }.start()
        ui.chatChattersInclude.searchChattersInclude.searchChattersLayout.animate().apply {
            duration = 500
            translationX(size.widthPixels.toFloat())
        }.withEndAction {
            ui.chatChattersInclude.searchChattersInclude.searchBar.text = null
            ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = null
            ui.chatChattersInclude.searchChattersInclude.searchChattersLayout.visibility = View.INVISIBLE
            ui.chatChattersInclude.searchChattersInclude.chatterNotFound.visibility = View.INVISIBLE
            ui.chatChattersInclude.searchChattersButton.isEnabled = true
        }.start()
    }
    private fun hint(message: String) {
        Pop.pop(this, message)
    }
    private fun load(state: Boolean) {
        if(state) ui.chatChattersInclude.loadingChatters.visibility = View.VISIBLE
        else ui.chatChattersInclude.loadingChatters.visibility = View.INVISIBLE
    }
    private fun manageOtherLayouts() {
        ui.chatterAiInclude.chatterAiLayout.translationY = size.heightPixels.toFloat()*-1
        ui.verificationLayoutInclude.verificationLayout.translationY = size.heightPixels.toFloat()
        ui.chatChattersInclude.searchChattersInclude.searchChattersLayout .translationX = size.heightPixels.toFloat()
    }
    override fun onResume() {
        super.onResume()
        ProfileActivity.layoutIsEnabled(ui.chatChattersInclude.chatChattersLayout,
            ui.chatChattersInclude.searchChattersInclude.searchChattersLayout, true)
        chatter = Chatter(
            fullName = shared.getString("fullName", null),
            username = shared.getString("username", null),
            email = shared.getString("email", null),
            phoneNumber = shared.getString("phoneNumber", null),
            birth = shared.getString("birth", null),
            gender = shared.getString("gender", null),
            profilePicture = shared.getString("profilePicture", null),
            password = null,
            token = null
        )
        PreRegex.me = shared.getString("profilePicture", "")?:""
        setChatterHeaderInfo()
        setChattersAdapter()
    }
    override fun onPause() {
        super.onPause()
        chatChattersListener?.remove()
    }
    override fun onDestroy() {
        super.onDestroy()
        database.collection("Chatters").document(chatter?.username!!).update("Available", FieldValue.serverTimestamp())
    }
    companion object {
        //This could have had multiple uses, just too lazy to refactor the code//
        fun pictureNullCheck(toCheck: Chatter, toApply: ImageView, filter: Int, context: Context) {
            if(!toCheck.profilePicture.isNullOrBlank()) toApply.setImageBitmap(ChattersAdapter.decodeImage(toCheck.profilePicture))
            else {
                when(toCheck.gender){
                    "Male" -> toApply.setImageResource(R.drawable.male_user_icon)
                    else -> toApply.setImageResource(R.drawable.female_user_icon)
                }
                when(filter){
                    0 -> toApply.colorFilter = null
                    1 -> toApply.setColorFilter(ContextCompat.getColor(context, R.color.seriousYellow))
                }
            }
        }
    }
}