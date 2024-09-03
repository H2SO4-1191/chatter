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
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ActivityLoggedBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Pop
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoggedActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var ui: ActivityLoggedBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var shared: SharedPreferences
    private val scope = MainScope()
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
        scope.launch { signedIn() }
    }
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
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
            hint("Hello, ${chatter?.fullName}.")
            storeData()
        }
        if(auth.currentUser?.isEmailVerified == true) {
            pass()
            getToken()
        }
        else verification()
    }
    private fun pass() {
        ui.verificationLayoutInclude.verificationLayout.visibility = View.GONE
        setDrawer()
        setChatterHeaderInfo()
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
                scope.launch {
                    load(true)
                    database.collection("Chatters").document(chatter?.username!!).update("FCMToken", FieldValue.delete()).await()
                    auth.signOut()
                    load(false)
                    shared.edit().clear().apply()
                    startActivity(Intent(this@LoggedActivity, MainActivity::class.java))
                    finish()
                }
            },
            onNo = {})
    }
    @SuppressLint("SetTextI18n")
    private fun verification() {
        fun load(state: Boolean) {
            if(state) {
                ui.verificationLayoutInclude.verifying.visibility = View.VISIBLE
                ui.verificationLayoutInclude.verifyButton.visibility = View.INVISIBLE
            } else {
                ui.verificationLayoutInclude.verifying.visibility = View.INVISIBLE
                ui.verificationLayoutInclude.verifyButton.visibility = View.VISIBLE
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
            hint("Verification email had been sent to ${chatter?.email}")
            load(false)
        }
        val verifyLayoutBinding = ui.verificationLayoutInclude
        val verifyView = ui.verificationLayoutInclude.verificationLayout
        verifyView.translationY = size.heightPixels.toFloat()
        verifyLayoutBinding.askToVerify.text = "${ContextCompat.getString(this, R.string._verifyEmail)} \"${chatter?.email}\" ${ContextCompat.getString(this, R.string.verifyEmail_)}"
        verifyView.visibility = View.VISIBLE
        verifyView.animate().apply {
            duration = 1000
            translationY(0f)
        }.start()
        verifyLayoutBinding.verifyButton.setOnClickListener {
            scope.launch {
                reload()
                if(auth.currentUser?.isEmailVerified == true) {
                    verifyLayoutBinding.verifyButton.visibility = View.INVISIBLE
                    verifyLayoutBinding.done.visibility = View.VISIBLE
                    verifyView.animate().apply {
                        duration = 2000
                        translationY(size.heightPixels.toFloat())
                    }.withEndAction { verifyView.visibility = View.GONE }.start()
                } else sendEmail()
            }
        }

    }
    private fun setChatterHeaderInfo() {
        val drawerInfo = ui.navigationDrawer.getHeaderView(0)
        drawerInfo.findViewById<TextView>(R.id.userNameHeader).text = chatter?.username
        drawerInfo.findViewById<TextView>(R.id.fullNameHeader).text = chatter?.fullName
        val profilePicture = drawerInfo.findViewById<ImageView>(R.id.profilePictureHeader)
        if(chatter?.profilePicture != null) profilePicture.setImageBitmap(getRoundedCornerBitmap(decodeImage(chatter?.profilePicture!!)))
        else {
            when(chatter?.gender){
                "Male" -> profilePicture.setImageResource(R.drawable.male_user_icon)
                else -> profilePicture.setImageResource(R.drawable.female_user_icon)
            }
        }
    }
    private fun decodeImage(imageString: String): Bitmap {
        val bytes = Base64.decode(imageString, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return bitmap
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
            scope.launch {
                when(menuItem.title) {
                    "Log-Out" -> logOut()
                    else -> hint(menuItem.title.toString())
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
        if(state) ui.loggedContent.loading.visibility = View.VISIBLE
        else ui.loggedContent.loading.visibility = View.INVISIBLE
    }
}