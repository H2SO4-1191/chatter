package com.h2so4.chatter.activities

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
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
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.h2so4.chatter.R
import com.h2so4.chatter.databinding.ActivitySignupBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Data.countries
import com.h2so4.chatter.models.Pop
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.*
import java.util.Calendar

class SignupActivity : AppCompatActivity() {

    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val newChatter = Chatter(null, null, null, null, null, null, null, null)
    private lateinit var ui: ActivitySignupBinding
    private val size = DisplayMetrics()
    private var steps = 1
    private var isAnimating: Boolean = false
    private var usernames: ArrayList<String>? = null
    private var emails: ArrayList<String>? = null
    private var phoneNumbers:ArrayList<String>? = null
    private var signedUp: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(ui.root)
        auth = FirebaseAuth.getInstance()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        windowManager.defaultDisplay.getRealMetrics(size)
        pen()
        getInfo()
        setListeners()
    }

    private fun getInfo() {
        usernames = intent.getStringArrayListExtra("usernames")
        emails = intent.getStringArrayListExtra("emails")
        phoneNumbers = intent.getStringArrayListExtra("phoneNumbers")
    }
    private fun pen() {
        hint("The pen will be your guide, tap it when guidance is needed.", "Hint")
        penHint(ui.fullNameField)
        ui.pen.rotation = 135f
        ui.pen.post(Runnable {
            ui.pen.height = ui.gap.height*0.25f.roundToInt()
            ui.pen.animate().apply {
                duration = 1250
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
    private fun penHint(target: View) {
        val hint: String = when(target){
            ui.fullNameField -> "Full name has to be in the form of \"Firstname Lastname\".\ne.g: Mustafa Muhammad."
            ui.userNameField -> "Username has to be unique, over 3 characters and have no spaces.\ne.g: H2SO4-1191."
            ui.emailField -> "Email has to be unique and in the form of an actual email address.\ne.g: example@example.example."
            ui.phoneNumberField -> "Phone number has to be unique, without spaces and in the form of an actual phone number with \'+\' and country code so do not write the trunk prefix (the first \'0\').\n-Tap the map for country codes.\ne.g: +964??????????."
            ui.passwordField -> "Password has to of 8 characters or more and have both digits and letters.\ne.g: 1q2w3e4r."
            ui.confirmPasswordField -> "Confirm password has to match the password."
            ui.birth -> "Tap the Birth field and pick your birth from the calender, tap the year to scroll through years faster.\ne.g: 07/01/2004"
            ui.profilePicture -> "Tap on the circle to add a profile picture, or hold the pen and skip it."
            else -> "Tap on one of the buttons to choose your gender, male or female."
        }
        ui.pen.setOnClickListener { hint(hint, "Hint") }
    }
    private fun setListeners() {
        infoChecker(ui.fullNameField, "^(?!.*\\d)[^\\d\\s]+\\s[^\\d\\s]+\$", ui.userNameField)
        infoChecker(ui.userNameField, "^[A-Za-z0-9!@#\$%^&*()_+={}\\[\\]:;\"'<>,.?\\/\\\\|`~\\-]{3,}\$", ui.emailField)
        infoChecker(ui.emailField, "[a-zA-Z0-9._-]+@[a-zA-Z]+\\.+[a-zA-Z]+", ui.phoneNumberField)
        infoChecker(ui.phoneNumberField, "^\\+\\d{10,15}\$", ui.passwordField)
        setCountryCode()
        setShowPassword(ui.showPassword, ui.passwordField)
        setShowPassword(ui.showConfirmPassword, ui.confirmPasswordField)
        infoChecker(ui.passwordField, "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", ui.confirmPasswordField)
        ui.birth.setOnFocusChangeListener { _, hasFocus -> if(hasFocus) pickDate() }
        ui.birth.setOnClickListener { pickDate() }
        infoChecker(ui.confirmPasswordField, "", ui.birth)
        infoChecker(ui.birth, "\\d{2}/\\d{2}/\\d{4}", ui.maleButton)
        ui.maleButton.setOnClickListener { gender(ui.maleButton) }
        ui.femaleButton.setOnClickListener { gender(ui.femaleButton) }
        ui.profilePicture.setOnClickListener { setPP() }
    }
    private fun infoChecker(target: TextView, regex: String, next: View) {
        val co: View? = when(target) {
            ui.emailField -> ui.countryCode
            ui.phoneNumberField -> ui.showPassword
            ui.passwordField -> ui.showConfirmPassword
            else -> null
        }
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
        val handler = Handler(Looper.getMainLooper())
        var runnable: Runnable? = null
        target.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                runnable?.let { handler.removeCallbacks(it) }
            }
            override fun afterTextChanged(s: Editable?) {
                runnable = Runnable {
                    if(target == ui.confirmPasswordField && !isAnimating && !next.isVisible) {
                        if(target.text.toString() == ui.passwordField.text.toString()) step(next, co)
                    } else {
                        if(target.text.toString().matches(regex.toRegex()) && !isAnimating && !next.isVisible) {
                            if(unique) {
                                if(isAvailable(target.text.toString(), type)) {
                                    setThings(target)
                                    step(next, co)
                                } else hint("$type is already registered.", "Error")
                            } else {
                                setThings(target)
                                step(next, co)
                            }
                        } else if(target.text.toString().isNotBlank()) hint("Invalid format", "Error")
                    }
                }
                handler.postDelayed(runnable!!, 1500)
            }
        })
    }
    private fun step(next: View, co: View?) {
        penHint(next)
        if(next == ui.maleButton) steps++
        ui.pen.animate().apply {
            isAnimating = true
            duration = 1000
            translationY((ui.fly.height.toFloat() * -1) + (ui.drop.height.toFloat() + ui.userNameField.height.toFloat()*steps + ui.gap.height.toFloat()*steps - ui.pen.height.toFloat()*0.1f))
        }.start()
        next.animate().apply {
            duration = if(next == ui.maleButton) 1500 else 1000
            next.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
            co?.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
            if(next == ui.maleButton) ui.femaleButton.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
        }.withEndAction {
            next.isVisible = true
            if(next == ui.maleButton) ui.femaleButton.isVisible = true
            co?.isVisible = true
            isAnimating = false
        }.start()
        steps++
    }
    private fun isAvailable(target: String, type: String): Boolean {
        val list = when(type) {
            "Username" -> usernames
            "Email" -> emails
            else -> phoneNumbers
        }
        return !list?.any { it.equals(target, ignoreCase = true) }!!
    }
    private fun setCountryCode() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries.keys.toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ui.countryCode.adapter = adapter
        ui.countryCode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                ui.phoneNumberField.setText(countries[parent.getItemAtPosition(position).toString()].toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    private fun setShowPassword(button: Button, text: TextView) {
        button.setOnClickListener {
            if(text.inputType == 129) text.inputType = 1
            else text.inputType = 129
        }
    }
    private fun pickDate() {
        val calendar = Calendar.getInstance()
        val dateDialog = DatePickerDialog(this, R.style.CustomDatePickerTheme, { _, selectedYear, selectedMonth, selectedDay ->
            var selectedDayA = selectedDay.toString()
            var selectedMonthA = (selectedMonth + 1).toString()
            if(selectedDayA.length == 1) selectedDayA = "0".plus(selectedDayA)
            if(selectedMonthA.length == 1) selectedMonthA = "0".plus(selectedMonthA)
            val date = "${selectedDayA}/${selectedMonthA}/${selectedYear}"
            ui.birth.setText(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        dateDialog.datePicker.maxDate = calendar.timeInMillis
        dateDialog.window?.setBackgroundDrawableResource(R.drawable.spinner_background)
        dateDialog.show()
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
            done()
            penHint(ui.profilePicture)
            ui.profilePicture.foreground = pp
        } else {
            penHint(ui.maleButton)
            newChatter.gender = ""
            ui.profilePicture.foreground = ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24)
        }
        if(!ui.profilePicture.isVisible) {
            ui.profilePicture.animate().apply {
                duration = 1000
                ui.profilePicture.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
            }.withEndAction {
                ui.profilePicture.isVisible = true
            }.start()
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
                ui.pen.setOnLongClickListener { false }
                ui.pen.setOnClickListener { confirm() }
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
    private fun setUpChatter() {
        newChatter.fullName = ui.fullNameField.text.toString()
        newChatter.username = ui.userNameField.text.toString()
        newChatter.email = ui.emailField.text.toString()
        newChatter.phoneNumber = ui.phoneNumberField.text.toString()
        newChatter.password = ui.passwordField.text.toString()
        newChatter.birth = ui.birth.text.toString()
    }
    private fun done() {
        hint("To skip profile picture hold the pen.", "Hint")
        penHint(ui.profilePicture)
        ui.pen.setOnLongClickListener {
            confirm()
            true
        }
    }
    private fun confirm() {
        val message = if(ui.profilePicture.foreground == ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24)) "Are you certain of your information?\nNote: Profile picture is optional" else "Are you certain of your information?"
        showYesNoDialog(this, message,
            onYes = { signup() },
            onNo = { hint("Double check your information please.", "Hint") }
        )
    }
    private fun manualCheckHelper(input: String, type: String,regex: String, unique: Boolean): Boolean {
        var pass = true
        if(!input.matches(regex.toRegex())) {
            pass = false
            hint("Invalid username format.", "Error")
        }
        if(unique) {
            if(!isAvailable(input, type)) {
                pass = false
                hint("$type is already registered.", "Error")
            }
        }
        return pass
    }
    private fun finalManualInDepthCheck(): Boolean {
        var finalSay = true
        if(!manualCheckHelper(ui.fullNameField.text.toString(), "FullName", "^(?!.*\\d)[^\\d\\s]+\\s[^\\d\\s]+\$", false)) finalSay = false
        if(!manualCheckHelper(ui.userNameField.text.toString(), "Username", "^[A-Za-z0-9!@#\$%^&*()_+={}\\[\\]:;\"'<>,.?\\/\\\\|`~\\-]{3,}\$", true)) finalSay = false
        if(!manualCheckHelper(ui.emailField.text.toString(), "Email", "[a-zA-Z0-9._-]+@[a-zA-Z]+\\.+[a-zA-Z]+", true)) finalSay = false
        if(!manualCheckHelper(ui.phoneNumberField.text.toString(), "PhoneNumber", "^\\+\\d{10,15}\$", true)) finalSay = false
        if(!manualCheckHelper(ui.passwordField.text.toString(), "Password", "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", false)) finalSay = false
        if(ui.confirmPasswordField.text.toString() != ui.passwordField.text.toString()) hint("Passwords do not match.", "Error")
        if(ui.birth.text.toString().isBlank()) hint("Birthdate is not provided.", "Error")
        if(newChatter.gender == null) hint("Gender is not provided.", "Error")
        return finalSay
    }
    private fun signup() {
        if(finalManualInDepthCheck()) {
            setUpChatter()
            if(isEmailRegistered(newChatter.email!!)) hint("Email is already registered", "Error")
            else sendVerificationEmail(newChatter.email!!, newChatter.password!!)
        }
    }
    private fun pushToDatabase() {
        database = FirebaseFirestore.getInstance()
        val newChatterInfo = hashMapOf(
            "FullName" to newChatter.fullName,
            "Username" to newChatter.username,
            "Email" to newChatter.email,
            "PhoneNumber" to newChatter.phoneNumber,
            "Password" to newChatter.password,
            "Birth" to newChatter.birth,
            "Gender" to newChatter.gender,
            "ProfilePicture" to newChatter.profilePicture
            )
        database.collection("Chatters").document(newChatter.username!!)
            .set(newChatterInfo)
            .addOnSuccessListener { wayBack(true) }
            .addOnFailureListener { e ->
                hint("Failed to sign you up, check your internet connection.\n${e.message.toString()}", "Error")
                ui.progressBar.isVisible = false
            }
    }
    private fun wayBack(result: Boolean) {
        signedUp = result
        ui.pen.animate().apply {
            duration = 2000
            translationY(size.heightPixels.toFloat())
        }.withEndAction {
            val backIntent = Intent()
            backIntent.putExtra("signedUp", signedUp);
            setResult(Activity.RESULT_OK, backIntent);
            finish()
        }
    }
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        showYesNoDialog(this, "Are you certain that you want to cancel signup?",
            onYes = {
                wayBack(false)
            },
            onNo = {}
        )
        if(false) super.onBackPressed()
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
    private fun hint(message: String, type: String?) { Pop.pop(this, "$type: $message") }
    private fun sendVerificationEmail(email: String, password: String) {
        if(!isEmailRegistered(email)) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        hint("Verification email will soon be sent to \"$email\".", "Hint")
                        val user = auth.currentUser
                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { emailTask ->
                                if (emailTask.isSuccessful) {
                                    hint("Verification email had been sent to \"$email\".\nPlease check your inbox to verify your email.", "Hint")
                                    pushToDatabase()
                                }
                                else {
                                    hint("Failed to send verification email.", "Error")
                                    ui.progressBar.isVisible = false
                                }
                            }
                    } else {
                        hint("Failed to check your email, check your internet connection.", "Error")
                        ui.progressBar.isVisible = false
                    }
                }
        }
    }
    private fun isEmailRegistered(email: String): Boolean {
        var itis = false
        ui.progressBar.isVisible = true
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result?.signInMethods.isNullOrEmpty()) itis = false
                    else itis = true
                } else {
                    hint("Failed to check your email, check your internet connection.", "Error")
                    ui.progressBar.isVisible = false
                }
            }
        return itis
    }
}
//        return auth.currentUser?.isEmailVerified ?: false        //

//try sign in first then send code if succeeded//