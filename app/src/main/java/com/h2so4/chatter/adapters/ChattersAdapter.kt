package com.h2so4.chatter.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.h2so4.chatter.BuildConfig
import com.h2so4.chatter.R
import com.h2so4.chatter.models.Chatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChattersAdapter(private val context: Context, val owner: Chatter, var chatters: ArrayList<Chatter>, val where: Int, private val click: (Chatter) -> Unit): RecyclerView.Adapter<ChattersAdapter.ChatterHolder>() {
    val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DB_URL)
    inner class ChatterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val picture: ImageView? = itemView.findViewById(R.id.chatterPictureIn)
        private val name: TextView? = itemView.findViewById(R.id.chatterUsername)
        private val other: TextView? = itemView.findViewById(R.id.chatterLastMessage)
        fun bindChatter(context: Context, chatter: Chatter) {
            if(!chatter.profilePicture.isNullOrBlank()) picture?.setImageBitmap(decodeImage(chatter.profilePicture))
            else {
                when(chatter.gender) {
                    "Male" -> picture?.setImageResource(R.drawable.male_user_icon)
                    else -> picture?.setImageResource(R.drawable.female_user_icon)
                }
                picture?.setColorFilter(ContextCompat.getColor(context, R.color.seriousYellow))
            }
            name?.text = chatter.username
            if(where == 0) other?.text = chatter.fullName
            else {
                CoroutineScope(Dispatchers.IO).launch {
                    var newMessages = 0
                    val reference1 = database.getReference("Chats").child("${owner.username}|${chatter.username}")
                    val check1 = reference1.get().await()
                    val reference2 = database.getReference("Chats").child("${chatter.username}|${owner.username}")
                    val check2 = reference2.get().await()
                    val get = if(check1.exists()) check1 else check2
                    for(i in get.children) if(i.child("sender").getValue(String::class.java) != owner.username
                        && i.child("state").getValue(Int::class.java) == 0) newMessages++
                    @SuppressLint("SetTextI18n")
                    other?.text = when(newMessages) {
                        0 -> "No new messages"
                        1 -> "1 New message"
                        else ->"$newMessages New messages"
                    }
                    if(newMessages > 0) other?.setTextColor(ContextCompat.getColor(context, R.color.white))
                    else other?.setTextColor(ContextCompat.getColor(context, R.color.seriousYellowAlpha))
                }
            }
            itemView.setOnClickListener { click(chatter) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatterHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.chat_chatters_chatter_cell, parent, false)
        return ChatterHolder(view)
    }
    override fun onBindViewHolder(holder: ChatterHolder, position: Int) {
        holder.bindChatter(context, chatters[position])
    }
    override fun getItemCount(): Int {
        return chatters.count()
    }

    companion object {
        fun decodeImage(imageString: String?): Bitmap {
            val bytes = Base64.decode(imageString, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            return bitmap
        }
    }
}

