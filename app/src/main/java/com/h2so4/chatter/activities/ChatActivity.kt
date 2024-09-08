package com.h2so4.chatter.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivityChatBinding
import com.h2so4.chatter.models.Chatter

class ChatActivity : AppCompatActivity() {

    private lateinit var ui: ActivityChatBinding
    private  var chatter: Chatter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityChatBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        chatter = intent.getParcelableExtra("chatter")
        setInfo()
    }
    private fun setInfo() {
        ui.chatterInfo.chatterUsername.text = chatter?.username
        if(chatter?.profilePicture != null && chatter?.profilePicture!!.isNotBlank()) ui.chatterInfo.chatterPictureIn.setImageBitmap(ChattersAdapter.decodeImage(chatter?.profilePicture))
        else {
            when(chatter?.gender){
                "Male" -> ui.chatterInfo.chatterPictureIn.setImageResource(R.drawable.male_user_icon)
                else -> ui.chatterInfo.chatterPictureIn.setImageResource(R.drawable.female_user_icon)
            }
            ui.chatterInfo.chatterPictureIn.setColorFilter(ContextCompat.getColor(this, R.color.seriousYellow))
        }
    }
}