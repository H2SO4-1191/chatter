package com.h2so4.chatter.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.AddedChattersAdapter
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivityProfileBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Pop
import com.h2so4.chatter.models.PreRegex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileActivity : BaseActivity() {

    private lateinit var ui: ActivityProfileBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var shared: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private val size = DisplayMetrics()
    private var inEdit = false
    private var visitor: Chatter? = null
    private var account: Chatter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        windowManager.defaultDisplay.getRealMetrics(size)
        database = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        visitor = intent.getParcelableExtra("visitor")
        visitor?.profilePicture = PreRegex.me
        account = intent.getParcelableExtra("account")
        account?.profilePicture = PreRegex.them
        ui.changePasswordInclude.changePasswordLayout.translationY = size.heightPixels.toFloat()
        ui.accountInfoContainer.translationX = size.widthPixels.toFloat()*1.25f
        ui.doButton.translationX = size.widthPixels.toFloat()*-1.25f
        ui.changePasswordInclude.changePasswordLayout.elevation = 10f
        setup()
        knockKnock()
    }

    @SuppressLint("SetTextI18n")
    private fun setup() {
        //Should have used getSharedPreferences when user is viewing his own profile (visitor == account) instead of database, but whatever//
        lifecycleScope.launch(Dispatchers.IO) {
            val info = database.collection("Chatters").document(account?.username!!).get().await()
            account?.fullName = info.getString("FullName")
            account?.email = info.getString("Email")
            account?.phoneNumber = info.getString("PhoneNumber")
            account?.birth = info.getString("Birth")
            withContext(Dispatchers.Main) {
                ui.loadingAccount.visibility = View.GONE
                ui.doButton.visibility = View.VISIBLE
                ui.accountInfoContainer.visibility = View.VISIBLE
                ui.doButton.animate().apply {
                    duration = 600
                    translationX(0f)
                }.start()
                ui.accountInfoContainer.animate().apply {
                    duration = 600
                    translationX(0f)
                }.start()
                if(!account?.profilePicture.isNullOrBlank()) { ui.profilePictureAccount.setImageBitmap(ChattersAdapter.decodeImage(account?.profilePicture)) }
                else {
                    when(account?.gender){
                        "Male" -> ui.profilePictureAccount.setImageResource(R.drawable.male_user_icon)
                        else -> ui.profilePictureAccount.setImageResource(R.drawable.female_user_icon)
                    }
                    ui.profilePictureAccount.background = null
                    ui.profilePictureAccount.setColorFilter(ContextCompat.getColor(this@ProfileActivity, R.color.seriousYellow))
                }
                ui.usernameAccountField.setText(account?.username)
                ui.fullNameAccountField.setText(account?.fullName)
                ui.emailAccountField.setText(account?.email)
                ui.phoneNumberAccountField.setText(account?.phoneNumber)
                if(visitor == null || visitor?.username == account?.username) ui.passwordAccountField.setText("******")
                else {
                    ui.passwordAccountText.visibility = View.GONE
                    ui.passwordAccountField.visibility = View.GONE
                }
                ui.birthAccountField.setText(account?.birth)
                ui.genderAccountField.setText(account?.gender)
                setupAddedChatters()
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setupAddedChatters() {
        val addedChatters = ArrayList<Chatter>()
        var getThem: QuerySnapshot?
        ui.loadingAdded.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            getThem = try { database.collection("Chatters").document(account?.username!!).collection("ChatChatters").get().await() }
            catch(e: Exception) { null }
            if(getThem?.isEmpty == true && getThem != null) {
                withContext(Dispatchers.Main) {
                    ui.addedChatters.visibility = View.GONE
                    ui.loadingAdded.visibility = View.GONE
                    ui.addedChattersText.text = "Doesn't chat with anyone yet."
                }
            } else {
                if(getThem != null) {
                    for (i in getThem!!) {
                        val contact = database.collection("Chatters").document(i.id).get().await()
                        addedChatters.add(
                            Chatter(
                                username = contact.getString("Username"),
                                profilePicture = contact.getString("ProfilePicture"),
                                gender = contact.getString("Gender"),
                                fullName = null, email = null, phoneNumber = null, password = null, birth = null, token = null
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    ui.loadingAdded.visibility = View.GONE
                    val foundChattersAdapter = AddedChattersAdapter(this@ProfileActivity, addedChatters) { account ->
                        if(ui.addedChatters.isEnabled) {
                            ui.addedChatters.isEnabled = false
                            val profileIntent = Intent(this@ProfileActivity, ProfileActivity::class.java)
                            PreRegex.me = visitor?.profilePicture?:""
                            visitor?.profilePicture = null
                            PreRegex.them = account.profilePicture?:""
                            account.profilePicture = null
                            profileIntent.putExtra("visitor", visitor)
                            profileIntent.putExtra("account", account)
                            startActivity(profileIntent)
                            visitor?.profilePicture = PreRegex.me
                            account.profilePicture = PreRegex.them
                            lifecycleScope.launch {
                                delay(1000)
                                ui.addedChatters.isEnabled = true
                            }
                        }
                    }
                    ui.addedChatters.adapter = foundChattersAdapter
                    ui.addedChatters.layoutManager = LinearLayoutManager(this@ProfileActivity, LinearLayoutManager.HORIZONTAL, false)
                    ui.addedChatters.setHasFixedSize(true)
                }
            }
        }
    }
    private fun knockKnock() {
        if(visitor == null || visitor?.username == account?.username) {
            ui.doButton.foreground = ContextCompat.getDrawable(this, R.drawable.baseline_edit_document_24)
            ui.doButton.setOnClickListener { edit() }
        } else {
            ui.doButton.foreground = ContextCompat.getDrawable(this, R.drawable.baseline_comment_24)
            ui.doButton.setOnClickListener {
                if(intent.getBooleanExtra("comeBack", false)) onBackPressed()
                else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        ui.doButton.isClickable = false
                        val chatIntent = Intent(this@ProfileActivity, ChatActivity::class.java)
                        PreRegex.me = visitor?.profilePicture?:""
                        PreRegex.them = account?.profilePicture?:""
                        visitor?.profilePicture = null
                        account?.profilePicture = null
                        chatIntent.putExtra("sender", visitor)
                        chatIntent.putExtra("receiver", account)
                        startActivity(chatIntent)
                        visitor?.profilePicture = PreRegex.me
                        account?.profilePicture = PreRegex.them
                        lifecycleScope.launch {
                            delay(1000)
                            ui.doButton.isClickable = true
                        }
                    }
                }
            }
        }
    }
    private fun edit() {
        inEdit = !inEdit
        shared = getSharedPreferences("chatter", Context.MODE_PRIVATE)
        fun change() {
            ui.loadingAccount.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                val newPP: String? = if(ui.profilePictureAccount.drawable != ContextCompat.getDrawable(this@ProfileActivity, R.drawable.male_user_icon) &&
                    ui.profilePictureAccount.drawable != ContextCompat.getDrawable(this@ProfileActivity, R.drawable.female_user_icon))
                    SignupActivity.encodeImage(ui.profilePictureAccount.drawable
                        .toBitmap(ui.profilePictureAccount.drawable.intrinsicWidth, ui.profilePictureAccount.drawable.intrinsicHeight))
                else null
                val changes = hashMapOf<String, Any?>(
                    "FullName" to ui.fullNameAccountField.text.toString(),
                    "Birth" to ui.birthAccountField.text.toString(),
                    "Gender" to ui.genderAccountField.text.toString(),
                    "ProfilePicture" to newPP
                )
                val editor = shared.edit()
                editor.putString("fullName", changes["FullName"].toString())
                editor.putString("birth", changes["Birth"].toString())
                editor.putString("gender", changes["Gender"].toString())
                editor.putString("profilePicture", newPP)
                editor.apply()
                database.collection("Chatters").document(account?.username!!).update(changes).await()
                withContext(Dispatchers.Main) { ui.loadingAccount.visibility = View.INVISIBLE }
            }
        }
        fun revert() {
            ui.fullNameAccountField.setText(account?.fullName)
            ui.birthAccountField.setText(account?.birth)
            ui.genderAccountField.setText(account?.gender)
            if(!account?.profilePicture.isNullOrBlank()) ui.profilePictureAccount.setImageBitmap(ChattersAdapter.decodeImage(account?.profilePicture))
            else {
                when(account?.gender){
                    "Male" -> ui.profilePictureAccount.setImageResource(R.drawable.male_user_icon)
                    else -> ui.profilePictureAccount.setImageResource(R.drawable.female_user_icon)
                }
                ui.profilePictureAccount.background = null
                ui.profilePictureAccount.setColorFilter(ContextCompat.getColor(this@ProfileActivity, R.color.seriousYellow))
            }
        }
        fun changePassword() {
            val include = ui.changePasswordInclude
            val layout = include.changePasswordLayout
            fun reveal() {
                if(include.currentPasswordChange.inputType == 129) {
                    include.currentPasswordChange.inputType = 1
                    include.newPasswordChange.inputType = 1
                    include.confirmNewPasswordChange.inputType = 1
                } else {
                    include.currentPasswordChange.inputType = 129
                    include.newPasswordChange.inputType = 129
                    include.confirmNewPasswordChange.inputType = 129
                }
            }
            fun move(state: Boolean) {
                if(state) {
                    layoutIsEnabled(ui.main, layout, false)
                    layout.visibility = View.VISIBLE
                    layout.animate().apply {
                        duration = 500
                        translationY(0f)
                    }.start()
                } else {
                    layoutIsEnabled(ui.main, layout, true)
                    layout.animate().apply {
                        duration = 500
                        translationY(size.heightPixels.toFloat())
                    }.withEndAction {
                        if(include.currentPasswordChange.inputType == 1) reveal()
                        include.currentPasswordChange.text = null
                        include.newPasswordChange.text = null
                        include.confirmNewPasswordChange.text = null
                        layout.visibility = View.INVISIBLE
                    }.start()
                }
            }
            fun forgotPassword() {
                include.forgotPasswordChange.isEnabled = false
                include.checkingPasswords.visibility = View.VISIBLE
                include.forgotPasswordChange.setTextColor(ContextCompat.getColor(this, R.color.offWhiteAlpha))
                lifecycleScope.launch(Dispatchers.IO) {
                    auth.sendPasswordResetEmail(account?.email!!).await()
                    withContext(Dispatchers.Main) {
                        include.checkingPasswords.visibility = View.INVISIBLE
                        hint("${ContextCompat.getString(this@ProfileActivity, R.string.reset_sent)}${account?.email!!}")
                        include.forgotPasswordChange.setTextColor(ContextCompat.getColor(this@ProfileActivity, R.color.seriousYellow))
                        for (count in 59 downTo 0) {
                            @SuppressLint("SetTextI18n")
                            include.forgotPasswordChange.text = "00:$count"
                            delay(1000)
                        }
                        include.forgotPasswordChange.isEnabled = true
                        include.forgotPasswordChange.text = ContextCompat.getString(this@ProfileActivity, R.string.forgot_your_password)
                    }
                }
            }
            fun submit() {
                if(include.currentPasswordChange.text.toString().isNotBlank() &&
                    include.newPasswordChange.text.toString().isNotBlank() &&
                    include.confirmNewPasswordChange.text.toString().isNotBlank()) {
                    var correct = true
                    include.btnSubmit.isEnabled = false
                    include.checkingPasswords.visibility = View.VISIBLE
                    layoutIsEnabled(layout, null, false)
                    lifecycleScope.launch(Dispatchers.IO) {
                        try { auth.signInWithEmailAndPassword(account?.email!!, include.currentPasswordChange.text.toString()).await() }
                        catch(e: Exception) {
                            correct = false
                            withContext(Dispatchers.Main) {
                                include.btnSubmit.isEnabled = false
                                include.checkingPasswords.visibility = View.INVISIBLE
                                hint(ContextCompat.getString(this@ProfileActivity, R.string.incorrect_password))
                                layoutIsEnabled(layout, null, true)
                            }
                        }
                        if(correct) {
                            if(include.newPasswordChange.text.toString().matches(PreRegex.password)) {
                                if(include.newPasswordChange.text.toString() == include.confirmNewPasswordChange.text.toString()) {
                                    auth.currentUser?.updatePassword(include.newPasswordChange.text.toString())?.await()
                                    withContext(Dispatchers.Main) {
                                        move(false)
                                        layoutIsEnabled(layout, null, true)
                                        hint(ContextCompat.getString(this@ProfileActivity, R.string.updated_password))
                                    }
                                } else withContext(Dispatchers.Main) { hint(ContextCompat.getString(this@ProfileActivity, R.string.passwords_not_match)) }
                            } else withContext(Dispatchers.Main) { hint(ContextCompat.getString(this@ProfileActivity, R.string.passwords_must)) }
                            withContext(Dispatchers.Main) {
                                include.btnSubmit.isEnabled = false
                                include.checkingPasswords.visibility = View.INVISIBLE
                                layoutIsEnabled(layout, null, true)
                            }
                        }
                    }
                } else hint(ContextCompat.getString(this, R.string.fill_first))
            }
            if(!layout.isVisible) {
                move(true)
                include.showAll.setOnClickListener { reveal() }
                include.forgotPasswordChange.setOnClickListener { forgotPassword() }
                include.btnSubmit.setOnClickListener { submit() }
                include.btnCancel.setOnClickListener { move(false) }
            } else move(false)
        }
        fun genderDialog() {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.pick_gender_layout, null)
            val dialog = AlertDialog.Builder(this).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.input_field)
            dialogView.findViewById<Button>(R.id.btnM).setOnClickListener {
                ui.genderAccountField.setText(ContextCompat.getString(this, R.string.male))
                dialog.dismiss()
            }
            dialogView.findViewById<Button>(R.id.btnF).setOnClickListener {
                ui.genderAccountField.setText(ContextCompat.getString(this, R.string.female))
                dialog.dismiss()
            }
            dialog.show()
        }
        fun setEditableInfo(field: EditText, pressed: Boolean?, tag: TextView, hint: String, state: Boolean) {
            field.isEnabled = state
            if(state) {
                when(pressed) {
                    true -> {
                        field.isFocusable = false
                        when(field) {
                            ui.passwordAccountField -> field.setOnClickListener { changePassword() }
                            ui.birthAccountField -> field.setOnClickListener { SignupActivity.pickDate(ui.birthAccountField, this) }
                            else -> field.setOnClickListener { genderDialog() }
                        }
                    }
                    false -> {
                        field.isFocusable = true
                        field.isFocusableInTouchMode = true
                    }
                    else -> {
                        field.isFocusable = false
                        field.setOnClickListener { hint(hint) }
                    }
                }
                tag.setOnClickListener { hint(hint) }
            } else {
                field.isFocusable = false
                tag.setOnClickListener {}
            }
        }
        fun setEditable(state: Boolean) {
            if(state) {
                hint(ContextCompat.getString(this, R.string.edit_mode))
                ui.addedChattersText.visibility = View.GONE
                ui.addedChatters.visibility = View.GONE
                ui.profilePictureAccount.setOnClickListener {
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
                    ui.profilePictureAccount.isClickable = false
                    lifecycleScope.launch {
                        delay(1000)
                        ui.profilePictureAccount.isClickable = true
                    }
                }
            } else {
                ui.addedChattersText.visibility = View.VISIBLE
                ui.addedChatters.visibility = View.VISIBLE
                ui.profilePictureAccount.setOnClickListener {}
            }
            setEditableInfo(ui.usernameAccountField, null, ui.usernameAccountText, ContextCompat.getString(this, R.string.cannot_be_edited), state)
            setEditableInfo(ui.fullNameAccountField, false, ui.fullNameAccountText, ContextCompat.getString(this, R.string.full_name_hint), state)
            setEditableInfo(ui.emailAccountField, null, ui.emailAccountText, ContextCompat.getString(this, R.string.cannot_be_edited), state)
            setEditableInfo(ui.phoneNumberAccountField, null, ui.phoneNumberAccountText, ContextCompat.getString(this, R.string.cannot_be_edited), state)
            setEditableInfo(ui.passwordAccountField, true, ui.passwordAccountText, ContextCompat.getString(this, R.string.password_hint), state)
            setEditableInfo(ui.birthAccountField, true, ui.birthAccountText, ContextCompat.getString(this, R.string.birth_hint), state)
            setEditableInfo(ui.genderAccountField, true, ui.genderAccountText, ContextCompat.getString(this, R.string.gender_hint), state)
        }
        lifecycleScope.launch(Dispatchers.Main) {
            if(inEdit) {
                ui.doButton.isClickable= false
                ui.doButton.animate().apply {
                    duration = 500
                    rotation(-720f)
                }.withEndAction {
                    ui.doButton.isClickable = true
                    setEditable(inEdit)
                }.start()
                delay(250)
                ui.doButton.foreground = ContextCompat.getDrawable(this@ProfileActivity, R.drawable.baseline_done_24)
            } else {
                SignupActivity.showYesNoDialog(this@ProfileActivity,
                    ContextCompat.getString(this@ProfileActivity, R.string.save_changes),
                    onYes = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            if(ui.fullNameAccountField.text.toString().matches(PreRegex.fullName)) {
                                change()
                                setEditable(inEdit)
                                ui.doButton.isClickable= false
                                ui.doButton.animate().apply {
                                    duration = 500
                                    rotation(0f)
                                }.withEndAction {
                                    ui.doButton.isClickable = true
                                }.start()
                                delay(250)
                                ui.doButton.foreground = ContextCompat.getDrawable(this@ProfileActivity, R.drawable.baseline_edit_document_24)
                            } else hint(ContextCompat.getString(this@ProfileActivity, R.string.full_name_hint))
                        }
                    },
                    onNo = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            revert()
                            setEditable(inEdit)
                            ui.doButton.isClickable= false
                            ui.doButton.animate().apply {
                                duration = 500
                                rotation(0f)
                            }.withEndAction {
                                ui.doButton.isClickable = true
                            }.start()
                            delay(250)
                            ui.doButton.foreground = ContextCompat.getDrawable(this@ProfileActivity, R.drawable.baseline_edit_document_24)
                        }
                    }
                )
            }
        }
    }
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                ui.profilePictureAccount.colorFilter = null
                ui.profilePictureAccount.setImageBitmap(bitmap)
            }
        } else if(requestCode != 1) hint("Permission required to access images.")
    }
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.", ReplaceWith("super.onBackPressed()", "androidx.appcompat.app.AppCompatActivity"))
    override fun onBackPressed() {
        if(ui.changePasswordInclude.changePasswordLayout.isVisible) {
            layoutIsEnabled(ui.main, null, true)
            ui.changePasswordInclude.changePasswordLayout.animate().apply {
                duration = 500
                translationY(size.heightPixels.toFloat())
            }.withEndAction {
                if(ui.changePasswordInclude.currentPasswordChange.inputType == 1){
                    SignupActivity.setShowPassword(ui.changePasswordInclude.showAll, ui.changePasswordInclude.currentPasswordChange)
                    SignupActivity.setShowPassword(ui.changePasswordInclude.showAll, ui.changePasswordInclude.newPasswordChange)
                    SignupActivity.setShowPassword(ui.changePasswordInclude.showAll, ui.changePasswordInclude.confirmNewPasswordChange)
                }
                ui.changePasswordInclude.currentPasswordChange.text = null
                ui.changePasswordInclude.newPasswordChange.text = null
                ui.changePasswordInclude.confirmNewPasswordChange.text = null
                ui.changePasswordInclude.changePasswordLayout.visibility = View.INVISIBLE
            }.start()
        } else if(inEdit) {
            SignupActivity.showYesNoDialog(this, ContextCompat.getString(this, R.string.discard_changes),
                onYes = { super.onBackPressed() },
                onNo = {})
        } else super.onBackPressed()
    }
    private fun hint(message: String) { Pop.pop(this, message) }
    companion object {
        fun layoutIsEnabled(layout: ViewGroup, exception: View?, state: Boolean) {
            for (i in layout.children) {
                if(i == exception) continue
                i.isEnabled = state
                if (i is ViewGroup) layoutIsEnabled(i, exception,state)
            }
        }
    }
}