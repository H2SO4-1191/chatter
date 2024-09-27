package com.h2so4.chatter.models

object PreRegex {
    val fullName = "^(?!.*\\d)[^\\d\\s]+\\s[^\\d\\s]+\$".toRegex()
    val username = "^[A-Za-z0-9_\\-]{3,}$".toRegex()
    val email = "[a-zA-Z0-9._-]+@[a-zA-Z]+\\.+[a-zA-Z]+".toRegex()
    val phoneNumber = "^\\+\\d{10,15}\$".toRegex()
    val password = "^.{6,}\$".toRegex()
    val birth = "\\d{2}/\\d{2}/\\d{4}".toRegex()

    var me = ""
    var them = ""
}