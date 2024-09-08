package com.h2so4.chatter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivityLoggedBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Pop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoggedActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var ui: ActivityLoggedBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var shared: SharedPreferences
    private lateinit var chattersAdapter: ChattersAdapter
    private lateinit var chatChatters: ArrayList<Chatter>
    private var size = DisplayMetrics()
    private var chatter: Chatter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityLoggedBinding.inflate(layoutInflater)
        setContentView(ui.root)
        shared = getSharedPreferences("chatter", Context.MODE_PRIVATE)
        windowManager.defaultDisplay.getRealMetrics(size)
        database = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        manageOtherLayouts()
        lifecycleScope.launch(Dispatchers.IO) { signedIn() }
    }

    private suspend fun signedIn() {
        if(intent.getBooleanExtra("logged", false)) {
            chatter = Chatter(
                fullName = shared.getString("fullName", null),
                username = shared.getString("username", null),
                email = shared.getString("email", null),
                phoneNumber = shared.getString("phoneNumber", null),
                password = null,
                birth = shared.getString("birth", null),
                gender = shared.getString("gender", null),
                profilePicture = shared.getString("profilePicture", null)
            )
        } else {
            chatter = intent.getParcelableExtra("chatter")
            withContext(Dispatchers.Main) { hint("Hello, ${chatter?.fullName}.") }
            storeData()
        }
        if(auth.currentUser?.isEmailVerified == true) {
            withContext(Dispatchers.Main) { pass() }
            getToken()
        } else verification()
    }
    private fun pass() {
        ui.verificationLayoutInclude.verificationLayout.visibility = View.GONE
        setDrawer()
        setChatterHeaderInfo()
        setChattersAdapter()
        setAddChatters()
    }
    private fun storeData() {
        val editor = shared.edit()
        editor.putString("fullName", chatter?.fullName)
        editor.putString("username", chatter?.username)
        editor.putString("email", chatter?.email)
        editor.putString("phoneNumber", chatter?.phoneNumber)
        editor.putString("birth", chatter?.birth)
        editor.putString("gender", chatter?.gender)
        editor.putString("profilePicture", chatter?.profilePicture)
        editor.apply()
    }
    private fun setChattersAdapter() {
        lifecycleScope.launch(Dispatchers.IO) {
            chatChatters = ArrayList()
            withContext(Dispatchers.Main) { load(true) }
            val search = database.collection("Chatters").document(chatter?.username!!).collection("ChatChatters").get().await()
            withContext(Dispatchers.Main) { load(false) }
            if (!search.isEmpty) {
                for (i in search) {
                    chatChatters.add(
                        Chatter(
                            username = i.getString("Username"),
                            profilePicture = i.getString("ProfilePicture"),
                            gender = i.getString("Gender"),
                            fullName = null, email = null, phoneNumber = null, password = null, birth = null
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    chattersAdapter = ChattersAdapter(this@LoggedActivity, chatChatters) { chatter ->
                        val chatIntent = Intent(this@LoggedActivity, ChatActivity::class.java)
                        chatIntent.putExtra("chatter" , chatter)
                        startActivity(chatIntent)
                    }
                    ui.chatChattersInclude.chattersList.adapter = chattersAdapter
                    ui.chatChattersInclude.chattersList.layoutManager = LinearLayoutManager(this@LoggedActivity)
                    ui.chatChattersInclude.chattersList.setHasFixedSize(true)
                }
            } else {
                withContext(Dispatchers.Main) { ui.chatChattersInclude.noChatters.visibility = View.VISIBLE }
            }
        }
    }
    private fun setAddChatters() {
        var toggled = false
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
                        if(i.getString("Username")?.contains(input) == true && i.getString("Username") != chatter?.username) {
                            foundChatters.add(
                                Chatter(
                                    fullName = i.getString("FullName"),
                                    username = i.getString("Username"),
                                    email = i.getString("Email"),
                                    phoneNumber = i.getString("PhoneNumber"),
                                    password = null,
                                    birth = i.getString("Birth"),
                                    gender = i.getString("Gender"),
                                    profilePicture = i.getString("ProfilePicture")
                                )
                            )
                        }
                    }
                    if(foundChatters.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            val foundChattersAdapter = ChattersAdapter(this@LoggedActivity, foundChatters) { chatter ->
                                //profileActivity
                            }
                            ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = foundChattersAdapter
                            ui.chatChattersInclude.searchChattersInclude.foundChatters.layoutManager = LinearLayoutManager(this@LoggedActivity)
                            ui.chatChattersInclude.searchChattersInclude.foundChatters.setHasFixedSize(true)
                        }
                    } else withContext(Dispatchers.Main) { ui.chatChattersInclude.searchChattersInclude.chatterNotFound.visibility = View.VISIBLE }
                }
            } else {
                search = null
                foundChatters.clear()
                ui.chatChattersInclude.searchChattersInclude.foundChatters.adapter = null
                ui.chatChattersInclude.searchChattersInclude.chatterNotFound.visibility = View.INVISIBLE
            }
        }
        add.setOnClickListener {
            if(!toggled) {
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
    private suspend fun logOut() {
        SignupActivity.showYesNoDialog(this, "Are you certain that you wish to log-out?",
            onYes = {
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) { onBackPressed() }
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
                                lifecycleScope.launch(Dispatchers.Main) { getToken() }
                            }.start()
                        }
                    } else sendEmail()
                }
            }
        }
    }
    private fun setChatterHeaderInfo() {
        val drawerInfo = ui.navigationDrawer.getHeaderView(0)
        drawerInfo.findViewById<TextView>(R.id.userNameHeader).text = chatter?.username
        drawerInfo.findViewById<TextView>(R.id.fullNameHeader).text = chatter?.fullName
        val profilePicture = drawerInfo.findViewById<ImageView>(R.id.profilePictureHeader)
        if(!chatter?.profilePicture.isNullOrBlank()) profilePicture.setImageBitmap(getRoundedCornerBitmap(ChattersAdapter.decodeImage(chatter?.profilePicture!!)))
        else {
            when(chatter?.gender){
                "Male" -> profilePicture.setImageResource(R.drawable.male_user_icon)
                else -> profilePicture.setImageResource(R.drawable.female_user_icon)
            }
        }
    }
    private fun getRoundedCornerBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        canvas.drawRoundRect(rect, 25f, 25f, paint)
        return output
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
                when(menuItem.title) {
                    "Log-Out" -> withContext(Dispatchers.Main) { logOut() }
                    else -> withContext(Dispatchers.Main) { hint(menuItem.title.toString()) }
                }
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
        if(drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
        super.onBackPressed()
    }
    private fun hint(message: String) {
        Pop.pop(this, message)
    }
    private fun load(state: Boolean) {
        if(state) ui.chatChattersInclude.loadingChatters.visibility = View.VISIBLE
        else ui.chatChattersInclude.loadingChatters.visibility = View.INVISIBLE
    }
    private fun manageOtherLayouts() {
        ui.verificationLayoutInclude.verificationLayout.translationY = size.heightPixels.toFloat()
        ui.chatChattersInclude.searchChattersInclude.searchChattersLayout .translationX = size.heightPixels.toFloat()
    }
}