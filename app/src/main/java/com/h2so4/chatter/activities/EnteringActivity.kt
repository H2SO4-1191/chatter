package com.h2so4.chatter.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginRight
import com.google.firebase.firestore.FirebaseFirestore
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ActivityEnteringBinding
import com.h2so4.chatter.models.Pop
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EnteringActivity : AppCompatActivity() {

    private lateinit var ui: ActivityEnteringBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var usernames: ArrayList<String>
    private lateinit var emails: ArrayList<String>
    private lateinit var phoneNumbers: ArrayList<String>
    private var signedUp: Boolean? = null
    private val size = DisplayMetrics()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityEnteringBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        windowManager.defaultDisplay.getRealMetrics(size)
        database = FirebaseFirestore.getInstance()
        downloadInfo()
        setFoundLogin()
        setLoginPressed()
        setSignUpPressed()
    }

    private fun setFoundLogin() {
        ui.password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(ui.password.text.toString() == "open") {
                    ui.loginDoor.isVisible = true
                    ui.loginDoor.animate().apply {
                        duration = 500
                        translationX(ui.loginDoor.marginRight.toFloat())
                    }.start()
                    ui.email.animate().apply {
                        duration = 500
                        translationY(-100f)
                    }.start()
                    ui.password.animate().apply {
                        duration = 500
                        translationY(100f)
                    }.start()
                } else if(ui.loginDoor.isVisible) {
                    ui.loginDoor.animate().apply {
                        duration = 500
                        translationX(0f)
                    }.start()
                    ui.email.animate().apply {
                        duration = 500
                        translationY(0f)
                    }.start()
                    ui.password.animate().apply {
                        duration = 500
                        translationY(0f)
                    }.withEndAction { ui.loginDoor.isVisible = false }.start()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun setLoginPressed() {
        ui.loginDoor.setOnClickListener {
            ui.loginDoor.animate().apply {
                duration = 500
                translationX(ui.loginDoor.marginRight.toFloat()*2f)
            }.start()
            ui.email.animate().apply {
                duration = 500
                translationY(ui.signUpPen.y*0.8f)
            }.start()
            ui.password.animate().apply {
                duration = 500
                translationY(ui.signUpPen.y*-0.5f)
            }.withEndAction {
                ui.email.isVisible = false
                ui.password.isVisible = false
                ui.loginDoor.isVisible = false
                val loggedIntent = Intent(this, LoggedActivity::class.java)
                startActivity(loggedIntent)
            }.start()
        }
    }
    private fun setSignUpPressed() {
        ui.signUpPen.setOnClickListener {
            ui.signUpPen.animate().apply {
                duration = 500
                rotation(135f)
            }.withEndAction {
                ui.signUpPen.animate().apply {
                    duration = 500
                    translationY(100f)
                }.withEndAction {
                    ui.signUpPen.animate().apply {
                        duration = 1000
                        translationY(size.heightPixels.toFloat() * -1)
                    }.withEndAction {
                        val signupIntent = Intent(this@EnteringActivity, SignupActivity::class.java)
                        signupIntent.putStringArrayListExtra("usernames", usernames)
                        signupIntent.putStringArrayListExtra("emails", emails)
                        signupIntent.putStringArrayListExtra("phoneNumbers", phoneNumbers)
                        startActivityForResult(signupIntent, 1)
                    }.start()
                }
            }
        }
    }
    private fun downloadInfo() {
        MainScope().launch {
            usernames = getField("Username")
            emails = getField("Email")
            phoneNumbers = getField("PhoneNumbers")
        }
    }
    private suspend fun getField(field: String): ArrayList<String> {
        return ArrayList(database.collection("Chatters").get().await().documents.mapNotNull { it.getString(field) })
    }
    private fun hint(message: String) { Pop.pop(this, message) }
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != Activity.RESULT_OK) return
        if(requestCode == 1) {
            signedUp = data?.getBooleanExtra("signedUp", false)
            if(signedUp == true) hint("You can now sign in to your Chatter account to continue verification.")
            ui.signUpPen.rotation = -45f
            ui.signUpPen.animate().apply {
                duration = 1500
                translationY(0f)
            }.start()
            ui.signUpPen.animate().apply {
                duration = 2000
                rotation(0f)
            }.start()
        }
    }
}