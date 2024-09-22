package com.h2so4.chatter.models

import com.google.firebase.Timestamp

data class Message(val sender: String = "null", val message: String = "null", var date: Any? = null){
    override fun toString(): String {
        return "Sender: $sender\nMessage: $message\nDate: $date"
    }
}