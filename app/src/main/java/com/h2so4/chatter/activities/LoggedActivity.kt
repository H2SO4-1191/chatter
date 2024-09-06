package com.h2so4.chatter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Bundle
import android.util.Base64
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
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
            pass()
            getToken()
        } else verification()
    }
    private fun pass() {
        ui.verificationLayoutInclude.verificationLayout.visibility = View.GONE
        setDrawer()
        setChatterHeaderInfo()
        setChattersAdapter()
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
            chatChatters = ArrayList<Chatter>()
            val search = database.collection("Chatters").document(chatter?.username!!)
                .collection("ChatChatters").get().await()
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
                    ui.chatChattersLayout.chattersList.adapter = chattersAdapter
                    ui.chatChattersLayout.chattersList.layoutManager = LinearLayoutManager(this@LoggedActivity)
                    ui.chatChattersLayout.chattersList.setHasFixedSize(true)
                }
            } else {
                withContext(Dispatchers.Main) { hint("LLL") }
            }
        }
    }
    private suspend fun getToken() {
        val token = FirebaseMessaging.getInstance().token.await()
        updateToken(token)
    }
    private suspend fun updateToken(token: String) {
        database.collection("Chatters").document(chatter?.username!!).update("FCMToken", token).await()
    }
    private suspend fun logOut() {
        SignupActivity.showYesNoDialog(this, "Are you certain that you wish to log-out?",
            onYes = {
                lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        load(true)
                        onBackPressed()
                    }
                    database.collection("Chatters").document(chatter?.username!!).update("FCMToken", FieldValue.delete()).await()
                    auth.signOut()
                    withContext(Dispatchers.Main) { load(false) }
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
            verifyView.translationY = size.heightPixels.toFloat()
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
        if(state) ui.loading.visibility = View.VISIBLE
        else ui.loading.visibility = View.INVISIBLE
    }
}