package com.h2so4.chatter.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.MessageReceivedCellBinding
import com.h2so4.chatter.databinding.MessageSentCellBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Message
import com.h2so4.chatter.models.Pop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val sender: Chatter, private val receiver: Chatter, val messages: ArrayList<Message>, val context: Context): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var senderPP =
        if(!sender.profilePicture.isNullOrBlank()) BitmapDrawable(context.resources, ChattersAdapter.decodeImage(sender.profilePicture))
        else {
            when(sender.gender){
                "Male" -> ContextCompat.getDrawable(context, R.drawable.male_user_icon)!!
                else -> ContextCompat.getDrawable(context, R.drawable.female_user_icon)!!
            }

        }
    private var receiverPP =
        if(!receiver.profilePicture.isNullOrBlank()) BitmapDrawable(context.resources, ChattersAdapter.decodeImage(receiver.profilePicture))
        else {
            when(receiver.gender){
                "Male" -> ContextCompat.getDrawable(context, R.drawable.male_user_icon)!!
                else -> ContextCompat.getDrawable(context, R.drawable.female_user_icon)!!
            }

        }

    inner class SendViewHolder(private val binding: MessageSentCellBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(message: Message) {
          binding.senderProfilePicture.setImageDrawable(senderPP)
          binding.messageCell.text = message.message
          binding.seen.visibility = if(message.state == 1) View.VISIBLE else View.INVISIBLE
          binding.date.text = setLocalTime(message.date as Long)
            binding.messageCell.setOnLongClickListener {
                copyToClipboard(message.message, context)
                true
            }
        }
    }
    inner class ReceiveViewHolder(private val binding: MessageReceivedCellBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(message: Message) {
            binding.senderChatterProfilePicture.setImageDrawable(receiverPP)
            binding.messageCellReceived.text = message.message
            binding.dateReceived.text = setLocalTime(message.date as Long)
            binding.messageCellReceived.setOnLongClickListener {
                copyToClipboard(message.message, context)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == 0) SendViewHolder(MessageSentCellBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
        else ReceiveViewHolder(MessageReceivedCellBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(getItemViewType(position) == 0) (holder as SendViewHolder).setData(messages[position])
        else (holder as ReceiveViewHolder).setData(messages[position])
    }

    override fun getItemViewType(position: Int): Int {
        return if(messages[position].sender == sender.username) 0 else 1
    }

    override fun getItemCount(): Int {
        return messages.count()
    }

    fun addMessage(newMessage: Message) {
        messages.add(newMessage)
        notifyItemInserted(messages.size - 1)
    }

    companion object {
        fun setLocalTime(timestamp: Long): String {
            return SimpleDateFormat("dd/MM/yyyy | HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        fun copyToClipboard(text: String, context: Context) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Message", text)
            clipboard.setPrimaryClip(clip)
            Pop.pop(context, "Text copied to clipboard.")
        }
    }
}