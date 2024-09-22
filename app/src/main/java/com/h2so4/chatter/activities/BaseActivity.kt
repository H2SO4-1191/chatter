package com.h2so4.chatter.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

open class BaseActivity: AppCompatActivity() {
    private lateinit var reference: DocumentReference
    private lateinit var shared:SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shared = getSharedPreferences("chatter", Context.MODE_PRIVATE)
        reference = FirebaseFirestore.getInstance().collection("Chatters").document(shared.getString("username", null)!!)
    }
    override fun onPause() {
        super.onPause()
        reference.update("Available", FieldValue.serverTimestamp())
    }
    override fun onResume() {
        super.onResume()
        reference.update("Available", 1)
    }
}