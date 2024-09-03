package com.h2so4.chatter.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        setChatterPressed()
    }
    private fun setChatterPressed() {
        ui.chatter.setOnClickListener {
            ui.chatter.isEnabled = false
            ui.chatter.animate().apply {
                duration = 2000
                rotationYBy(1440f)
            }.withEndAction {
                ui.chatter.isEnabled = true
                val signingIntent = Intent(this, EnteringActivity::class.java)
                startActivity(signingIntent)
            }.start()
        }
    }
}