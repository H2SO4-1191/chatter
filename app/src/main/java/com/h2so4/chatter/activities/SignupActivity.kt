package com.h2so4.chatter.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ActivitySignupBinding
import com.h2so4.chatter.models.Chatter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class SignupActivity : AppCompatActivity() {

    private lateinit var newChatter: Chatter
    private lateinit var ui: ActivitySignupBinding
    private val size = DisplayMetrics()
    private var steps = 1
    private  var isAnimating: Boolean = false
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(ui.root)
        database = FirebaseFirestore.getInstance()
        FirebaseApp.initializeApp(this)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        windowManager.defaultDisplay.getRealMetrics(size)
        pen()
        setListeners()

        //test
        printH(isAvailable("H2SO4-1191", "Username").toString())

    }

    private fun pen() {
        ui.pen.rotation = 135f
        ui.pen.post(Runnable {
            ui.pen.height = ui.gap.height*0.25f.roundToInt()
            ui.pen.animate().apply {
                duration = 1500
                translationY(ui.fly.height.toFloat() * -1)
            }.withEndAction {
                ui.pen.animate().apply {
                    duration = 2000
                    translationY((ui.fly.height.toFloat() * -1) + (ui.drop.height.toFloat() - ui.pen.height.toFloat()*0.1f))
                }
                ui.pen.animate().apply {
                    duration = 1000
                    rotation(-45f)
                }.withEndAction {
                    ui.fullNameField.animate().apply {
                        duration = 2000
                        ui.fullNameField.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
                    }.withEndAction {
                        ui.fullNameField.isVisible = true
                    }.start()
                }.start()
            }.start()
        })
    }
    private fun setListeners() {
        infoChecker(ui.fullNameField, "^[^\\s]+\\s[^\\s]+$", ui.userNameField)
        infoChecker(ui.userNameField, "^\\S{3,}\$", ui.emailField)
        infoChecker(ui.emailField, "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+", ui.phoneNumberField)
        infoChecker(ui.phoneNumberField, "\\d{10}", ui.passwordField)
        infoChecker(ui.passwordField, "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$", ui.confirmPasswordField)
        infoChecker(ui.confirmPasswordField, "", ui.birth)
        infoChecker(ui.birth, "\\d{2}/\\d{2}/\\d{4}", ui.maleButton)
        ui.maleButton.setOnClickListener { gender(ui.maleButton) }
        ui.femaleButton.setOnClickListener { gender(ui.femaleButton) }
        ui.pen.setOnClickListener { confirm() }
        ui.profilePicture.setOnClickListener { setPP() }
    }
    private fun infoChecker(target: TextView, regex: String, next: View) {
        var unique = false
        var type = ""
        if(target == ui.userNameField || target == ui.emailField || target == ui.phoneNumberField) {
            unique = true
            type = when(target) {
                ui.userNameField -> "Username"
                ui.emailField -> "Email"
                else -> "PhoneNumber"
            }
        }
        target.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(target == ui.confirmPasswordField) {
                    if(target.text.toString() == ui.passwordField.text.toString()) step(next)
                } else {
                    if(target.text.toString().matches(regex.toRegex()) && !isAnimating && !next.isVisible) {
                        if(unique) {
                            if(isAvailable(target.text.toString(), type, )) {
                                setThings(target)
                                step(next)
                            }
                        } else {
                            setThings(target)
                            step(next)
                        }
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun step(next: View) {
        if(next == ui.maleButton) steps++
        ui.pen.animate().apply {
            isAnimating = true
            duration = 1000
            translationY((ui.fly.height.toFloat() * -1) + (ui.drop.height.toFloat() + ui.userNameField.height.toFloat()*steps + ui.gap.height.toFloat()*steps - ui.pen.height.toFloat()*0.1f))
        }.start()
        next.animate().apply {
            duration = if(next == ui.maleButton) 1500 else 1000
            next.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
            if(next == ui.maleButton) ui.femaleButton.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
        }.withEndAction {
            next.isVisible = true
            isAnimating = false
        }.start()
        steps++
    }
    private fun isAvailable(target: String, type: String): Boolean {
        var isAvailable = true
        getField(type ,object: MyCallBack {
            override fun onCallback(field: List<String>) {
                if(field.contains(target)) {
                    printH("$type is already registered.")
                    isAvailable = false
                }
            }
        })
        return isAvailable
    }
    private fun getField(field: String, call: MyCallBack) {
        database.collection("Chatters").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val list = ArrayList<String>()
                for (document in task.result) list.add(document.data[field].toString())
                call.onCallback(list)
            }
        }
    }
    private fun gender(pressed: ToggleButton) {
        val pp: Drawable?
        if(pressed == ui.maleButton) {
            ui.femaleButton.isChecked = false
            newChatter.gender = "Male"
            pp = ContextCompat.getDrawable(this, R.drawable.male_user_icon)
        } else {
            ui.maleButton.isChecked = false
            newChatter.gender = "Female"
            pp = ContextCompat.getDrawable(this, R.drawable.female_user_icon)
        }
        if(pressed.isChecked) {
            ui.pen.isEnabled = true
            ui.pen.foregroundTintList = pressed.textColors
            ui.profilePicture.foreground = pp
        } else {
            ui.pen.isEnabled = false
            newChatter.gender = ""
            ui.pen.foregroundTintList = ContextCompat.getColorStateList(this, R.color.seriousYellow)
            ui.profilePicture.foreground = ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24)
        }
        if(!ui.profilePicture.isVisible) {
            ui.profilePicture.animate().apply {
                duration = 1000
                ui.profilePicture.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
            }.withEndAction {
                ui.profilePicture.isVisible = true
            }.start()
            printH("Tap the pen when ready.\n*Profile picture is optional.")
        }
    }
    private fun setPP() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1)
    }
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            printH(imageUri.toString())
            if (imageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
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
                    color = ContextCompat.getColor(this@SignupActivity, R.color.seriousYellow)
                    style = Paint.Style.STROKE
                    strokeWidth = 25f
                }
                canvas.drawCircle(radius, radius, radius - 5f, strokePaint)
                ui.profilePicture.foreground = BitmapDrawable(resources, output)
                ui.profilePicture.foregroundTintList = null
                newChatter.profilePicture = encodeImage(bitmap)
            }
        }
    }
    private fun encodeImage(bitmap: Bitmap): String {
        val previewHeight = bitmap.height*150/bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewHeight, 150, false)
        val byteArray = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArray)
        val bytes = byteArray.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
    private fun setThings(target: TextView) {
        when(target) {
            ui.fullNameField -> newChatter.fullName = target.text.toString()
            ui.userNameField -> newChatter.username = target.text.toString()
            ui.emailField -> newChatter.email = target.text.toString()
            ui.phoneNumberField -> newChatter.phoneNumber = target.text.toString()
            ui.passwordField -> newChatter.password = target.text.toString()
            ui.birth -> newChatter.birth = target.text.toString()
        }
    }
    private fun isEmailValid(email: String, password: String): Boolean {
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Send verification email
                    printH("GG")
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { emailTask ->
                            if (emailTask.isSuccessful) {
                                // Email sent
                                printH("SS")
                            } else {
                                // Error occurred
                                printH("HH")
                            }
                        }
                } else {
                    // Error occurred
                    printH("LL")
                }
            }
        return auth.currentUser?.isEmailVerified ?: false
    }
    private fun isPhoneNumberValid(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) { /* auto-retrieval */ }
                override fun onVerificationFailed(e: FirebaseException) { /* handle error */ }
                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) { /* save verificationId and token */ }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private fun finalCheckUp(email: String, password: String, phoneNumber: String): Boolean {
        var so = true
        if(!isEmailValid(email, password)) {
            printH("Something is wrong with the email.")
            so = false
        }
        isPhoneNumberValid(phoneNumber)
        if(phoneNumber != FirebaseAuth.getInstance().currentUser?.phoneNumber) {
            printH("Something is wrong with the phone number.")
            so = false
        }
        return so
    }
    private fun signup() {
        if(finalCheckUp(newChatter.email!!, newChatter.password!!, newChatter.phoneNumber!!)){
            val newChatterInfo = hashMapOf(
                "FullName" to newChatter.fullName,
                "Username" to newChatter.username,
                "Email" to newChatter.email,
                "PhoneNumber" to newChatter.phoneNumber,
                "Password" to newChatter.password,
                "birth" to newChatter.birth,
                "Gender" to newChatter.gender,
                "ProfilePicture" to newChatter.profilePicture,
                )
            database.collection("Chatters").document(newChatter.username!!).set(newChatterInfo)
                .addOnSuccessListener {
                    printH("Welcome to Chatter.")
                }
                .addOnFailureListener { e ->
                    printH("Something went wrong.\n${e.message}")
                }
        }
    }
    private fun confirm() {
        val message = if(ui.profilePicture.foreground == ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24)) "Are you certain of your information?\nNote: Profile picture is optional" else "Are you certain of your information?"
        showYesNoDialog(this, message,
            onYes = {
                signup()
                ui.pen.animate().apply {
                    duration = 5000
                    translationY(size.heightPixels.toFloat()/2f)
                }.withEndAction {
                    val loggedIntent = Intent(this, LoggedActivity::class.java)
                    startActivity(loggedIntent)
                }
            },
            onNo = { printH("Double check your information please.") }
        )
    }
    private fun showYesNoDialog(context: Context, message: String, onYes: () -> Unit, onNo: () -> Unit) {
        AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                onYes()
            }
            .setNegativeButton("No") { _, _ ->
                onNo()
            }
            .show()
    }
    private fun printH(text: String) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    interface MyCallBack { fun onCallback(field: List<String>) }
}
//TODO("Add birth calendar.")//
//FirebaseAuth.getInstance().currentUser?.isEmailVerified//