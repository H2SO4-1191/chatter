package com.h2so4.chatter.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.h2so4.chatter.R
import java.util.concurrent.TimeUnit

class VerificationActivity : AppCompatActivity() {

//    private lateinit var verificationId: String
//    private lateinit var resendingToken: PhoneAuthProvider.ForceResendingToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)

    }


//    private fun isPhoneNumberValid(phoneNumber: String) {
//        val options = PhoneAuthOptions.newBuilder(auth)
//            .setPhoneNumber(phoneNumber)
//            .setTimeout(60L, TimeUnit.SECONDS)
//            .setActivity(this)
//            .setCallbacks(callbacks)
//            .build()
//        PhoneAuthProvider.verifyPhoneNumber(options)
//        /////
//        val verificationId = "your_verification_id"
//        val code = "123456" // Code received by the user
//        val credential = PhoneAuthProvider.getCredential(verificationId, code)
//        signInWithPhoneAuthCredential(credential)
//    }
//    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
//        override fun onVerificationCompleted(credential: PhoneAuthCredential) { signInWithPhoneAuthCredential(credential) }
//        override fun onVerificationFailed(e: FirebaseException) { printH("Something went wrong: ${e.message.toString()}.") }
//        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
//            this@SignupActivity.verificationId = verificationId
//            this@SignupActivity.resendingToken = token
//        }
//    }
//    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
//        auth.signInWithCredential(credential)
//            .addOnCompleteListener(this) { task ->
//                if (task.isSuccessful) printH("PhoneNumber had been verified.")
//                else printH("Failed to verify PhoneNumber.")
//            }
//    }
//    private fun finalCheckUp(email: String, password: String, phoneNumber: String): Boolean {
//        var so = true
//        if(!isEmailValid(email, password)) {
//            printH("Something is wrong with the email.")
//            so = false
//        }
//        isPhoneNumberValid(phoneNumber)
//        if(phoneNumber != FirebaseAuth.getInstance().currentUser?.phoneNumber) {
//            printH("Something is wrong with the phone number.")
//            so = false
//        }
//        return so
//    }
//    private fun signup() {
//        if(finalCheckUp(newChatter.email!!, newChatter.password!!, newChatter.phoneNumber!!)){
//            val newChatterInfo = hashMapOf(
//                "FullName" to newChatter.fullName,
//                "Username" to newChatter.username,
//                "Email" to newChatter.email,
//                "PhoneNumber" to newChatter.phoneNumber,
//                "Password" to newChatter.password,
//                "birth" to newChatter.birth,
//                "Gender" to newChatter.gender,
//                "ProfilePicture" to newChatter.profilePicture,
//            )
//            database.collection("Chatters").document(newChatter.username!!).set(newChatterInfo)
//                .addOnSuccessListener {
//                    printH("Welcome to Chatter.")
//                }
//                .addOnFailureListener { e ->
//                    printH("Something went wrong.\n${e.message}")
//                }
//        }
//    }
}