package com.h2so4.chatter.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivityChatBinding
import com.h2so4.chatter.models.Chatter

class ChatActivity : AppCompatActivity() {

    private lateinit var ui: ActivityChatBinding
    private  var sender: Chatter? = null
    private  var receiver: Chatter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityChatBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        sender =  intent.getParcelableExtra("sender")
        receiver =  intent.getParcelableExtra("receiver")
        setInfo()
    }

    private fun setInfo() {
        ui.chatterInfo.chatterUsername.text = receiver?.username
        if(receiver?.profilePicture != null && receiver?.profilePicture!!.isNotBlank()) ui.chatterInfo.chatterPictureIn.setImageBitmap(ChattersAdapter.decodeImage(receiver?.profilePicture))
        else {
            when(receiver?.gender){
                "Male" -> ui.chatterInfo.chatterPictureIn.setImageResource(R.drawable.male_user_icon)
                else -> ui.chatterInfo.chatterPictureIn.setImageResource(R.drawable.female_user_icon)
            }
            ui.chatterInfo.chatterPictureIn.setColorFilter(ContextCompat.getColor(this, R.color.seriousYellow))
        }
        ui.profile.setOnClickListener {
            val profileIntent = Intent(this, ProfileActivity::class.java)
            profileIntent.putExtra("visitor", sender)
            profileIntent.putExtra("account", receiver)
            profileIntent.putExtra("comeBack", true)
            startActivity(profileIntent)
        }
    }
}