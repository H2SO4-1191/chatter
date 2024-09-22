package com.h2so4.chatter.models

data class NotificationRequest(
    val token: String,
    val title: String,
    val message: String
)
