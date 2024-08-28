package com.h2so4.chatter.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginRight
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ActivityEnteringBinding

class EnteringActivity : AppCompatActivity() {

    private lateinit var ui: ActivityEnteringBinding
    private val size = DisplayMetrics()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityEnteringBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        windowManager.defaultDisplay.getRealMetrics(size)
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
                        startActivity(signupIntent)
                    }.start()
                }
            }
        }
    }
}