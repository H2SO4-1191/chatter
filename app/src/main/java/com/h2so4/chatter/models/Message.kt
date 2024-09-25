package com.h2so4.chatter.models

data class Message(val sender: String = "null", val message: String = "null", var state: Int = 0, var date: Any? = null){
    override fun toString(): String {
        return "Sender: $sender\nMessage: $message\nDate: $date"
    }
}