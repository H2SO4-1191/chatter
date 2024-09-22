package com.h2so4.chatter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.ChattersAdapter.Companion.decodeImage
import com.h2so4.chatter.models.Chatter

class AddedChattersAdapter(private val context: Context, private val chatters: ArrayList<Chatter>, private val click: (Chatter) -> Unit): RecyclerView.Adapter<AddedChattersAdapter.AddedChatterHolder>() {
    inner class AddedChatterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val picture: ImageView? = itemView.findViewById(R.id.profilePictureAddedChatter)
        private val username: TextView? = itemView.findViewById(R.id.usernameAddedChatter)
        private val fullName: TextView? = itemView.findViewById(R.id.chatterLastMessage)
        fun bindChatter(context: Context, chatter: Chatter) {
            if(!chatter.profilePicture.isNullOrBlank()) picture?.setImageBitmap(decodeImage(chatter.profilePicture))
            else {
                when(chatter.gender) {
                    "Male" -> picture?.setImageResource(R.drawable.male_user_icon)
                    else -> picture?.setImageResource(R.drawable.female_user_icon)
                }
                picture?.setColorFilter(ContextCompat.getColor(context, R.color.seriousYellow))
            }
            username?.text = chatter.username
            fullName?.text = chatter.fullName
            itemView.setOnClickListener { click(chatter) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddedChatterHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.added_chatter_cell, parent, false)
        return AddedChatterHolder(view)
    }
    override fun onBindViewHolder(holder: AddedChatterHolder, position: Int) {
        holder.bindChatter(context, chatters[position])
    }
    override fun getItemCount(): Int {
        return chatters.count()
    }
}