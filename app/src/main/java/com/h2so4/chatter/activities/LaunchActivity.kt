package com.h2so4.chatter.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.h2so4.chatter.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("CustomSplashScreen")
class LaunchActivity : AppCompatActivity() {

    private lateinit var shared: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shared = getSharedPreferences("chatter", MODE_PRIVATE)
        checkFirstLaunch()
        determine()
    }
    private fun determine() {
        if(getUsername() == null) startActivity(Intent(this, MainActivity::class.java))
        else startActivity(Intent(this, LoggedActivity::class.java).putExtra("logged", true))
        finish()
    }
    private fun getUsername(): String? {
        val username = shared.getString("username", null)
        return username
    }
    private fun checkFirstLaunch() {
        val isFirstLaunch = shared.getBoolean("isFirstLaunch", true)
        if (isFirstLaunch) {
            lifecycleScope.launch {
                shared.edit().clear().apply()
                shared.edit().putBoolean("isFirstLaunch", false).apply()
                FirebaseFirestore.getInstance().clearPersistence().await()
            }
        }
    }
}