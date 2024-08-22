package com.h2so4.chatter.controller

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ActivitySignupBinding


class SignupActivity : AppCompatActivity() {

    private lateinit var ui: ActivitySignupBinding
    private val size = DisplayMetrics()
    private var steps = 1
    private  var isAnimating: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        windowManager.defaultDisplay.getRealMetrics(size)
        pen()
        downStairs()
    }

    private fun pen() {
        ui.pen.rotation = 135f
        ui.hello.post(Runnable {
            ui.pen.animate().apply {
                duration = 1500
                translationY(ui.fly.height.toFloat() * -1)
            }.withEndAction {
                ui.pen.animate().apply {
                    duration = 2000
                    translationY((ui.fly.height.toFloat() * -1) + (ui.drop.height.toFloat() - ui.pen.height.toFloat()*0.1f))
                }
                ui.pen.animate().apply {
                    duration = 1000
                    rotation(-45f)
                }.withEndAction {
                    ui.userNameField.animate().apply {
                        duration = 2000
                        ui.userNameField.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
                    }.withEndAction {
                        ui.userNameField.isVisible = true
                    }.start()
                }.start()
            }.start()
        })
    }
    private fun downStairs() {
        changeListener(ui.userNameField, "H", ui.emailField)
        changeListener(ui.emailField, "@", ui.phoneNumber)
        changeListener(ui.phoneNumber, "9", ui.passwordField)
        changeListener(ui.passwordField, "n", ui.confirmPasswordField)
        changeListener(ui.confirmPasswordField, "n", ui.birth)
        changeListener(ui.birth, "7", ui.maleButton)
        ui.maleButton.setOnClickListener {
            ui.femaleButton.isChecked = false
            if(ui.maleButton.isChecked) ui.pen.isEnabled = true
            else ui.pen.isEnabled = false
        }
        ui.femaleButton.setOnClickListener {
            ui.maleButton.isChecked = false
            if(ui.femaleButton.isChecked) ui.pen.isEnabled = true
            else ui.pen.isEnabled = false
        }
        ui.pen.setOnClickListener {
            showYesNoDialog(this, "Are you certain of your information?",
                onYes = {
                    ui.pen.animate().apply {
                        duration = 1000
                        translationY(size.heightPixels.toFloat()/2f)
                    }.withEndAction {
                        Toast.makeText(this, "Signed-Up!", Toast.LENGTH_SHORT).show()
                    }
                },
                onNo = {
                    // Handle "No" action
                }
            )
        }
    }
    private fun changeListener(target: TextView, key: String, next: View) {
        target.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(target.text.toString() == key && !isAnimating && !next.isVisible) step(next)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun step(next: View) {
        if(next == ui.maleButton) steps++
        ui.pen.animate().apply {
            isAnimating = true
            duration = 1000
            translationY((ui.fly.height.toFloat() * -1) + (ui.drop.height.toFloat() + ui.userNameField.height.toFloat()*steps + ui.gap.height.toFloat()*steps - ui.pen.height.toFloat()*0.1f))
        }.withEndAction {
            next.animate().apply {
                duration = if(next == ui.maleButton) 1500 else 1000
                next.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
                if(next == ui.maleButton) ui.femaleButton.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
            }.withEndAction {
                next.isVisible = true
                isAnimating = false
            }.start()
        }.start()
        steps++
    }
    private fun showYesNoDialog(context: Context, message: String, onYes: () -> Unit, onNo: () -> Unit) {
        AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                onYes()
            }
            .setNegativeButton("No") { _, _ ->
                onNo()
            }
            .show()
    }
}