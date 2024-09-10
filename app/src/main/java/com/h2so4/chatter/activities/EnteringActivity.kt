package com.h2so4.chatter.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginRight
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ActivityEnteringBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Pop
import com.h2so4.chatter.models.PreRegex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EnteringActivity : AppCompatActivity() {

    private lateinit var ui: ActivityEnteringBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var chatter: Chatter
    private var signedUp: Boolean? = null
    private val size = DisplayMetrics()
    private var user: String? = null
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityEnteringBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        windowManager.defaultDisplay.getRealMetrics(size)
        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
        setListeners()
    }

    private fun setListeners() {
        setChangeListener(ui.inputField)
        setChangeListener(ui.passwordField)
        setSignUpPressed()
        login()
        SignupActivity.setShowPassword(ui.showPassword, ui.passwordField)
        lifecycleScope.launch(Dispatchers.IO) { setForgotPassword() }
    }
    private fun setChangeListener(target: TextView) {
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        target.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                runnable?.let { handler.removeCallbacks(it) }
            }
            override fun afterTextChanged(s: Editable?) {
                runnable = Runnable {
                    lifecycleScope.launch(Dispatchers.IO) {
                        if(!ui.progressBar.isVisible) {
                            if (target == ui.inputField) withContext(Dispatchers.Main) { fetchLogin(target.text.toString().uppercase()) }
                            else withContext(Dispatchers.Main) { tryLogin(target.text.toString(), false) }
                            if(target.text.toString().isBlank()) withContext(Dispatchers.Main) { loginDoorMove("dismiss") }
                        }
                    }
                }
                handler.postDelayed(runnable!!, 1500)
            }
        })
    }
    private fun getType(input: String): String {
        return if (input.matches(PreRegex.username)) "Username"
        else if (input.matches(PreRegex.email)) "Email"
        else if (input.matches(PreRegex.phoneNumber)) "PhoneNumber"
        else "Invalid"
    }
    private suspend fun fetchLogin(input: String): Boolean {
        if(input.isBlank()) return false
        val type = getType(input)
        if (type == "Invalid") {
            user = null
            email = null
            auth.signOut()
            hint("Invalid login format")
            loginDoorMove("dismiss")
            errorShake(ui.inputField)
            return false
        }
        ui.progressBar.visibility = View.VISIBLE
        val search = database.collection("Chatters").whereEqualTo(type, input).get().await()
        ui.progressBar.visibility = View.INVISIBLE
        if (search.isEmpty) {
            user = null
            email = null
            auth.signOut()
            hint("No login found.")
            loginDoorMove("dismiss")
            errorShake(ui.inputField)
            return false
        }
        hint("Login found.")
        user = input
        email = search.documents.firstOrNull()?.getString("Email")
        tryLogin(ui.passwordField.text.toString(), false)
        getOtherInfo(search)
        return true
    }
    private suspend fun tryLogin(password: String, next: Boolean) {
        if (password.matches(PreRegex.password) && password.isNotBlank()) {
            if (email != null) {
                try {
                    ui.progressBar.visibility = View.VISIBLE
                    auth.signInWithEmailAndPassword(email!!, password).await()
                    ui.progressBar.visibility = View.INVISIBLE
                    loginDoorMove("come")
                    if(next) login()
                } catch (e: Exception) {
                    auth.signOut()
                    loginDoorMove("dismiss")
                    hint("Incorrect Password")
                    errorShake(ui.passwordField)
                    ui.progressBar.visibility = View.INVISIBLE
                }
            } else {
                errorShake(ui.inputField)
                hint("Enter a valid login first.")
            }
        } else if(password.isNotBlank()) {
            errorShake(ui.passwordField)
            hint("Invalid password format.")
        }
    }
    private suspend fun setForgotPassword() {
        ui.forgotPassword.setOnClickListener {
            if (email == null) {
                errorShake(ui.inputField)
                hint("Enter a valid login first.")
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        ui.forgotPassword.isEnabled = false
                        ui.progressBar.visibility = View.VISIBLE
                        auth.sendPasswordResetEmail(email!!).await()
                        hint("Reset email had been sent to $email")
                        ui.progressBar.visibility = View.INVISIBLE
                        ui.forgotPassword.isEnabled = true
                    } catch (e: Exception) {
                        hint("Failed reset email.")
                        ui.progressBar.visibility = View.INVISIBLE
                        ui.forgotPassword.isEnabled = true
                    }
                }
            }
        }

    }
    private fun login() {
        ui.loginDoor.setOnClickListener {
            ui.loginDoor.animate().apply {
                duration = 500
                translationX(ui.loginDoor.marginRight.toFloat() * 2f)
            }.start()
            ui.inputField.animate().apply {
                duration = 500
                translationY(0f)
            }.start()
            ui.showPassword.animate().apply {
                duration = 500
                translationY(0f)
            }.start()
            ui.forgotPassword.animate().apply {
                duration = 500
                translationY(0f)
            }.start()
            ui.passwordField.animate().apply {
                duration = 500
                translationY(0f)
            }.withEndAction {
                ui.loginDoor.visibility = View.INVISIBLE
                val loggedIntent = Intent(this, LoggedActivity::class.java)
                loggedIntent.putExtra("logged", false)
                loggedIntent.putExtra("chatter", chatter)
                startActivity(loggedIntent)
                finish()
            }.start()
        }
    }
    private fun getOtherInfo(search: QuerySnapshot) {
        chatter = Chatter(
            fullName = search.documents.firstOrNull()?.getString("FullName"),
            username = search.documents.firstOrNull()?.getString("Username"),
            email = search.documents.firstOrNull()?.getString("Email"),
            phoneNumber = search.documents.firstOrNull()?.getString("PhoneNumber"),
            password = null,
            birth = search.documents.firstOrNull()?.getString("Birth"),
            gender = search.documents.firstOrNull()?.getString("Gender"),
            profilePicture = search.documents.firstOrNull()?.getString("ProfilePicture")
        )
    }
    private fun setSignUpPressed() {
        ui.signUpPen.setOnClickListener {
            ui.signUpPen.isEnabled = false
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
                        ui.signUpPen.isEnabled = true
                        val signupIntent = Intent(this@EnteringActivity, SignupActivity::class.java)
                        startActivityForResult(signupIntent, 1)
                    }.start()
                }
            }
        }
    }
    private fun loginDoorMove(action: String) {
        if (action == "come") {
            ui.loginDoor.isEnabled = true
            ui.loginDoor.visibility = View.VISIBLE
            ui.loginDoor.animate().apply {
                duration = 500
                translationX(ui.loginDoor.marginRight.toFloat())
            }.start()
            ui.inputField.animate().apply {
                duration = 500
                translationY(-100f)
            }.start()
            ui.passwordField.animate().apply {
                duration = 500
                translationY(100f)
            }.start()
            ui.showPassword.animate().apply {
                duration = 500
                translationY(100f)
            }.start()
            ui.forgotPassword.animate().apply {
                duration = 500
                translationY(100f)
            }.start()
        } else if (action == "dismiss") {
            ui.loginDoor.isEnabled = false
            ui.loginDoor.animate().apply {
                duration = 500
                translationX(0f)
            }.start()
            ui.inputField.animate().apply {
                duration = 500
                translationY(0f)
            }.start()
            ui.passwordField.animate().apply {
                duration = 500
                translationY(0f)
            }.start()
            ui.showPassword.animate().apply {
                duration = 500
                translationY(0f)
            }.start()
            ui.forgotPassword.animate().apply {
                duration = 500
                translationY(0f)
            }.withEndAction { ui.loginDoor.visibility = View.INVISIBLE }.start()
        }
    }
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        if (requestCode == 1) {
            signedUp = data?.getBooleanExtra("signedUp", false)
            if (signedUp == true) hint("You can now sign in to your Chatter account to continue verification.")
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
    private fun hint(message: String) {
        Pop.pop(this, message)
    }
    private fun errorShake(view: View) {
        view.animate().apply {
            duration = 50
            translationX(25f)
        }.withEndAction {
            view.animate().apply {
                duration = 100
                translationX(-25f)
            }.withEndAction {
                view.animate().apply {
                    duration = 100
                    translationX(25f)
                }.withEndAction {
                    view.animate().apply {
                        duration = 50
                        translationX(0f)
                    }.start()
                }
            }
        }
        if(view == ui.passwordField) errorShake(ui.showPassword)
    }
}