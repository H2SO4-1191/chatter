package com.h2so4.chatter.activities

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivitySignupBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Codes.countries
import com.h2so4.chatter.models.Pop
import com.h2so4.chatter.models.PreRegex
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class SignupActivity : AppCompatActivity() {

    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val newChatter = Chatter(null, null, null, null, null, null, null, null, null)
    private lateinit var ui: ActivitySignupBinding
    private val size = DisplayMetrics()
    private var steps = 1
    private var isAnimating: Boolean = false
    private var signedUp: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(ui.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        windowManager.defaultDisplay.getRealMetrics(size)
        pen()
        setListeners()
    }

    private fun pen() {
        hint(ContextCompat.getString(this, R.string.pen_guide), "Hint")
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
                        duration = 1000
                        ui.fullNameField.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
                    }.withEndAction {
                        ui.fullNameField.visibility = View.VISIBLE
                    }.start()
                }.start()
            }.start()
        })
    }
    private fun penHint(target: View) {
        val hint: String = when(target){
            ui.fullNameField -> ContextCompat.getString(this, R.string.full_name_hint)
            ui.userNameField -> ContextCompat.getString(this, R.string.username_hint)
            ui.emailField -> ContextCompat.getString(this, R.string.email_hint)
            ui.phoneNumberField -> ContextCompat.getString(this, R.string.phone_hint)
            ui.passwordField -> ContextCompat.getString(this, R.string.password_hint)
            ui.confirmPasswordField -> ContextCompat.getString(this, R.string.confirm_password_hint)
            ui.birth -> ContextCompat.getString(this, R.string.birth_hint)
            ui.profilePicture -> ContextCompat.getString(this, R.string.profile_picture_hint)
            else -> ContextCompat.getString(this, R.string.gender_hint)
        }
        ui.pen.setOnClickListener { hint(hint, "Hint") }
    }
    private fun setListeners() {
        infoChecker(ui.fullNameField, PreRegex.fullName, ui.userNameField)
        infoChecker(ui.userNameField, PreRegex.username, ui.emailField)
        infoChecker(ui.emailField, PreRegex.email, ui.phoneNumberField)
        infoChecker(ui.phoneNumberField, PreRegex.phoneNumber, ui.passwordField)
        setCountryCode()
        setShowPassword(ui.showPassword, ui.passwordField)
        setShowPassword(ui.showConfirmPassword, ui.confirmPasswordField)
        infoChecker(ui.passwordField, PreRegex.password, ui.confirmPasswordField)
        ui.birth.setOnClickListener { pickDate(ui.birth, this) }
        infoChecker(ui.confirmPasswordField, ui.passwordField.text.toString().toRegex(), ui.birth)
        infoChecker(ui.birth, PreRegex.birth, ui.maleButton)
        ui.maleButton.setOnClickListener { gender(ui.maleButton) }
        ui.femaleButton.setOnClickListener { gender(ui.femaleButton) }
        ui.profilePicture.setOnClickListener {
            ui.profilePicture.isClickable = false
            setPP()
            lifecycleScope.launch {
                delay(1000)
                ui.profilePicture.isClickable = true
            }
        }
    }
    private fun infoChecker(target: TextView, regex: Regex, next: View) {
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
                        if(target.text.toString() == ui.passwordField.text.toString()) {
                            step(next, co)
                            val imm = this@SignupActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(target.windowToken, 0)
                        }
                        else hint(ContextCompat.getString(this@SignupActivity, R.string.confirm_password_hint), "Error")
                    } else {
                        if(target.text.toString().matches(regex) && !isAnimating && !next.isVisible) {
                            if(unique) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) {
                                        if(isAvailable(target.text.toString(), type, true)) {
                                            setThings(target)
                                            step(next, co)
                                            val imm = this@SignupActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                            imm.hideSoftInputFromWindow(target.windowToken, 0)
                                        } else hint("$type ${ContextCompat.getString(this@SignupActivity, R.string.already_registered)}", "Error")
                                    }
                                }
                            } else {
                                setThings(target)
                                step(next, co)
                                val imm = this@SignupActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.hideSoftInputFromWindow(target.windowToken, 0)
                            }
                        } else if(target.text.toString().isNotBlank() && !isAnimating && !next.isVisible) hint(ContextCompat.getString(this@SignupActivity, R.string.invalid_format), "Error")
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
            next.visibility = View.VISIBLE
            if(next == ui.maleButton) ui.femaleButton.visibility = View.VISIBLE
            co?.visibility = View.VISIBLE
            isAnimating = false
        }.start()
        steps++
    }
    private suspend fun isAvailable(target: String, type: String, check: Boolean): Boolean {
        var available = true
        var checking: View? = null
        if(check) {
             checking = when(type) {
                "Username" -> ui.checkingUsername
                "Email" -> ui.checkingEmail
                "PhoneNumber" -> ui.checkingPhoneNumber
                else -> null
            }
            checking?.visibility = View.VISIBLE
        }
        val query = database.collection("Chatters").whereEqualTo(type, target.uppercase()).get().await()
        if(!query.isEmpty) available = false
        checking?.visibility = View.INVISIBLE
        return available
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
            ui.profilePicture.foregroundTintList = ui.pen.foregroundTintList
        } else {
            penHint(ui.maleButton)
            newChatter.gender = null
            ui.profilePicture.foreground = ContextCompat.getDrawable(this, R.drawable.baseline_account_circle_24)
            ui.pen.setOnLongClickListener { false }
        }
        if(!ui.profilePicture.isVisible) {
            ui.profilePicture.animate().apply {
                duration = 1000
                ui.profilePicture.startAnimation(AnimationUtils.loadAnimation(this@SignupActivity, R.anim.fade_in))
            }.withEndAction {
                ui.profilePicture.visibility = View.VISIBLE
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }.start()
        }
    }
    private fun setPP() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, 1)
            } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                ui.profilePicture.setImageBitmap(bitmap)
                ui.profilePicture.foregroundTintList = null
                ui.profilePicture.foreground = null
                newChatter.profilePicture = encodeImage(bitmap)
                ui.pen.setOnLongClickListener { false }
                ui.pen.setOnClickListener { confirm() }
            }
        } else if(requestCode != 1) hint("Permission required to access images.", "Error")
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
        if(!ui.profilePicture.isVisible) hint(ContextCompat.getString(this, R.string.to_skip), "Hint")
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
            onNo = { hint(ContextCompat.getString(this, R.string.double_check), "Hint") }
        )
    }
    private fun manualCheckHelper(input: String, type: String,regex: Regex, unique: Boolean): Boolean {
        var pass = true
        if(!input.matches(regex)) {
            pass = false
            hint("Invalid $type format.", "Error")
        }
        if(unique) {
            lifecycleScope.launch(Dispatchers.IO) {
                if(!isAvailable(input, type, false)) {
                    pass = false
                    hint("$type ${ContextCompat.getString(this@SignupActivity, R.string.already_registered)}", "Error")
                }
            }
        }
        return pass
    }
    private fun finalManualInDepthCheck(): Boolean {
        var finalSay = true
        if(!manualCheckHelper(ui.fullNameField.text.toString(), "FullName", PreRegex.fullName, false)) finalSay = false
        if(!manualCheckHelper(ui.userNameField.text.toString(), "Username", PreRegex.username, true)) finalSay = false
        if(!manualCheckHelper(ui.emailField.text.toString(), "Email", PreRegex.email, true)) finalSay = false
        if(!manualCheckHelper(ui.phoneNumberField.text.toString(), "PhoneNumber", PreRegex.phoneNumber, true)) finalSay = false
        if(!manualCheckHelper(ui.passwordField.text.toString(), "Password", PreRegex.password, false)) finalSay = false
        if(ui.confirmPasswordField.text.toString() != ui.passwordField.text.toString()) hint(ContextCompat.getString(this, R.string.passwords_not_match), "Error")
        if(ui.birth.text.toString().isBlank()) hint("Birthdate is not provided.", "Error")
        if(newChatter.gender == null) hint("Gender is not provided.", "Error")
        return finalSay
    }
    private fun signup() {
        if(finalManualInDepthCheck()) {
            setUpChatter()
            ui.pen.isEnabled = false
            ui.progressBar.visibility = View.VISIBLE
            checkAndSendVerificationEmail(newChatter.email!!, newChatter.password!!)
        }
    }
    private fun pushToDatabase() {
        database = FirebaseFirestore.getInstance()
        val newChatterInfo = hashMapOf(
            "FullName" to newChatter.fullName,
            "Username" to newChatter.username?.uppercase(),
            "Email" to newChatter.email?.uppercase(),
            "PhoneNumber" to newChatter.phoneNumber,
            "Birth" to newChatter.birth,
            "Gender" to newChatter.gender,
            "ProfilePicture" to newChatter.profilePicture,
            "Available" to FieldValue.serverTimestamp()
            )
        database.collection("Chatters").document(newChatter.username?.uppercase()!!)
            .set(newChatterInfo)
            .addOnSuccessListener {
                wayBack(true)
            }
            .addOnFailureListener { e ->
                hint("Failed to sign you up, check your internet connection.\n${e.message.toString()}", "Error")
                ui.progressBar.visibility = View.INVISIBLE
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
            onYes = { wayBack(false) },
            onNo = {}
        )
        if(false) super.onBackPressed()
    }
    private fun hint(message: String, type: String?) { Pop.pop(this, "$type: $message") }
    private fun checkAndSendVerificationEmail(email: String, password: String) {
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
                                ui.pen.isEnabled = true
                                ui.progressBar.visibility = View.INVISIBLE
                            }
                        }
                } else {
                    hint("Email address is already associated with a Chatter account, try another account please.", "Error")
                    ui.pen.isEnabled = true
                    ui.progressBar.visibility = View.INVISIBLE
                }
            }
    }
    companion object {
        fun encodeImage(bitmap: Bitmap): String {
            val maxSize = 1024
            val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (width, height) = if (bitmap.width > bitmap.height)maxSize to (maxSize / aspectRatio).toInt()
            else (maxSize * aspectRatio).toInt() to maxSize
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
            val byteArray = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArray)
            val bytes = byteArray.toByteArray()
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }
        fun pickDate(view: TextView, context: Context) {
            val calendar = Calendar.getInstance()
            val dateDialog = DatePickerDialog(context, R.style.CustomDatePickerTheme, { _, selectedYear, selectedMonth, selectedDay ->
                var selectedDayA = selectedDay.toString()
                var selectedMonthA = (selectedMonth + 1).toString()
                if(selectedDayA.length == 1) selectedDayA = "0".plus(selectedDayA)
                if(selectedMonthA.length == 1) selectedMonthA = "0".plus(selectedMonthA)
                val date = "${selectedDayA}/${selectedMonthA}/${selectedYear}"
                view.text = date
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            dateDialog.datePicker.maxDate = calendar.timeInMillis
            dateDialog.window?.setBackgroundDrawableResource(R.drawable.spinner_background)
            dateDialog.show()
        }
        fun setShowPassword(button: Button, text: TextView) {
            button.setOnClickListener {
                if(text.inputType == 129) text.inputType = 1
                else text.inputType = 129
            }
        }
        fun showYesNoDialog(context: Context, message: String, onYes: () -> Unit, onNo: () -> Unit) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.yes_no, null)
            dialogView.findViewById<TextView>(R.id.dialogMessage).text = message
            val dialog = AlertDialog.Builder(context).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.input_field)
            dialogView.findViewById<Button>(R.id.btnYes).setOnClickListener {
                onYes()
                dialog.dismiss()
            }
            dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener {
                onNo()
                dialog.dismiss()
            }
            dialog.show()
        }
    }
}