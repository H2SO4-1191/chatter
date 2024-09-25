package com.h2so4.chatter.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.h2so4.chatter.R

class AboutActivity : BaseActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<TextView>(R.id.aboutApp).text =
            ContextCompat.getString(this, R.string.aboutApp1)
                .plus(ContextCompat.getString(this, R.string.aboutApp2))
                .plus(ContextCompat.getString(this, R.string.aboutApp3))
    }
}