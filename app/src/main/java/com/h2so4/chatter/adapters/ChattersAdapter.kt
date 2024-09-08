package com.h2so4.chatter.adapters

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
import com.h2so4.chatter.R
import com.h2so4.chatter.models.Chatter

class ChattersAdapter(private val context: Context, private val chatters: ArrayList<Chatter>, private val click: (Chatter) -> Unit): RecyclerView.Adapter<ChattersAdapter.ChatterHolder>() {
    inner class ChatterHolder(itemView: View, click: (Chatter) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val picture: ImageView? = itemView.findViewById(R.id.chatterPictureIn)
        private val name: TextView? = itemView.findViewById(R.id.chatterUsername)
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
            itemView.setOnClickListener { click(chatter) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatterHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.chat_chatters_chatter_cell, parent, false)
        return ChatterHolder(view, click)
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

