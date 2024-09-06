package com.h2so4.chatter.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ChatChattersChatterCellBinding
import com.h2so4.chatter.models.Chatter

class ChattersAdapter(private val context: Context, private val chatters: ArrayList<Chatter>, private val click: (Chatter) -> Unit): RecyclerView.Adapter<ChattersAdapter.ChatterHolder>() {
    inner class ChatterHolder(itemView: View, click: (Chatter) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val picture: ImageView? = itemView.findViewById(R.id.chatterPictureIn)
        private val name: TextView? = itemView.findViewById(R.id.chatterUsername)
        fun bindChatter(context: Context, chatter: Chatter) {
            if(!chatter.profilePicture.isNullOrBlank()) picture?.setImageBitmap(getCircleBitmap(decodeImage(chatter.profilePicture), context))
            else {
                when(chatter.gender){
                    "Male" -> picture?.setImageResource(R.drawable.male_user_icon)
                    else -> picture?.setImageResource(R.drawable.female_user_icon)
                }
                picture?.foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.seriousYellow))
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
        fun getCircleBitmap(bitmap: Bitmap, context: Context): Bitmap {
            val size = bitmap.width.coerceAtMost(bitmap.height)
            val xOffset = (bitmap.width - size) / 2f
            val yOffset = (bitmap.height - size) / 2f
            val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val matrix = Matrix()
            matrix.setTranslate(-xOffset, -yOffset)
            shader.setLocalMatrix(matrix)
            val paint = Paint().apply {
                isAntiAlias = true
                setShader(shader)
            }
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, paint)
            val strokePaint = Paint().apply {
                isAntiAlias = true
                color = ContextCompat.getColor(context, R.color.black)
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            canvas.drawCircle(radius, radius, radius - 5f, strokePaint)
            return output
        }
    }
}

