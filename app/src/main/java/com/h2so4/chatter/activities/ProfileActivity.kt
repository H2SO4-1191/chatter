package com.h2so4.chatter.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.h2so4.chatter.R
import com.h2so4.chatter.adapters.AddedChattersAdapter
import com.h2so4.chatter.adapters.ChattersAdapter
import com.h2so4.chatter.databinding.ActivityProfileBinding
import com.h2so4.chatter.models.Chatter
import com.h2so4.chatter.models.Pop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var ui: ActivityProfileBinding
    private lateinit var database: FirebaseFirestore
    private var inEdit = false
    private var visitor: Chatter? = null
    private var account: Chatter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(ui.root)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black)
        database = FirebaseFirestore.getInstance()
        visitor = intent.getParcelableExtra("visitor")
        account = intent.getParcelableExtra("account")
        setup()
        knockKnock()
    }

    @SuppressLint("SetTextI18n")
    private fun setup() {
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
                for (i in getThem!!) {
                    val contact = database.collection("Chatters").document(i.id).get().await()
                    addedChatters.add(
                        Chatter(
                            username = contact.getString("Username"),
                            profilePicture = contact.getString("ProfilePicture"),
                            gender = contact.getString("Gender"),
                            fullName = null, email = null, phoneNumber = null, password = null, birth = null
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    ui.loadingAdded.visibility = View.GONE
                    val foundChattersAdapter = AddedChattersAdapter(this@ProfileActivity, addedChatters) { account ->
                        val profileIntent = Intent(this@ProfileActivity, ProfileActivity::class.java)
                        profileIntent.putExtra("visitor", visitor)
                        profileIntent.putExtra("account", account)
                        startActivity(profileIntent)
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
                    lifecycleScope.launch(Dispatchers.IO) {
                        database.collection("Chatters").document(visitor?.username!!)
                            .collection("ChatChatters").document(account?.username!!).set(emptyMap<Any, Any>()).await()
                        withContext(Dispatchers.Main) {
                            val chatIntent = Intent(this@ProfileActivity, ChatActivity::class.java)
                            chatIntent.putExtra("sender", visitor)
                            chatIntent.putExtra("receiver", account)
                            startActivity(chatIntent)
                        }
                    }
                }
            }
        }
    }
    private fun edit() {
        inEdit = !inEdit
        fun change() {
            //shared & database//
        }
        fun revert() {
            ui.usernameAccountField.setText(account?.username)
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

        }
        @SuppressLint("SetTextI18n")
        fun genderDialog() {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.pick_gender_layout, null)
            val dialog = AlertDialog.Builder(this).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.input_field)
            dialogView.findViewById<Button>(R.id.btnM).setOnClickListener {
                ui.genderAccountField.setText("Male")
                dialog.dismiss()
            }
            dialogView.findViewById<Button>(R.id.btnF).setOnClickListener {
                ui.genderAccountField.setText("Female")
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
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    startActivityForResult(intent, 1)
                }
            } else {
                ui.addedChattersText.visibility = View.VISIBLE
                ui.addedChatters.visibility = View.VISIBLE
                ui.profilePictureAccount.setOnClickListener {}
            }
            setEditableInfo(ui.usernameAccountField, false, ui.usernameAccountText, ContextCompat.getString(this, R.string.username_hint), state)
            setEditableInfo(ui.fullNameAccountField, false, ui.fullNameAccountText, ContextCompat.getString(this, R.string.full_name_hint), state)
            setEditableInfo(ui.emailAccountField, null, ui.emailAccountText, ContextCompat.getString(this, R.string.cannot_be_edited), state)
            setEditableInfo(ui.phoneNumberAccountField, null, ui.phoneNumberAccountText, ContextCompat.getString(this, R.string.cannot_be_edited), state)
            setEditableInfo(ui.passwordAccountField, true, ui.passwordAccountText, ContextCompat.getString(this, R.string.password_hint), state)
            setEditableInfo(ui.birthAccountField, true, ui.birthAccountText, ContextCompat.getString(this, R.string.birth_hint), state)
            setEditableInfo(ui.genderAccountField, true, ui.genderAccountText, ContextCompat.getString(this, R.string.gender_hint), state)
        }
        lifecycleScope.launch(Dispatchers.Main) {
            if(inEdit) {
                ui.doButton.isEnabled = false
                ui.doButton.animate().apply {
                    duration = 500
                    rotation(-720f)
                }.withEndAction {
                    ui.doButton.isEnabled = true
                    setEditable(inEdit)
                }.start()
                delay(250)
                ui.doButton.foreground = ContextCompat.getDrawable(this@ProfileActivity, R.drawable.baseline_done_24)
            } else {
                SignupActivity.showYesNoDialog(this@ProfileActivity,
                    ContextCompat.getString(this@ProfileActivity, R.string.save_changes),
                    onYes = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            change()
                            setEditable(inEdit)
                            ui.doButton.isEnabled = false
                            ui.doButton.animate().apply {
                                duration = 500
                                rotation(0f)
                            }.withEndAction {
                                ui.doButton.isEnabled = true
                            }.start()
                            delay(250)
                            ui.doButton.foreground = ContextCompat.getDrawable(this@ProfileActivity, R.drawable.baseline_edit_document_24)
                        }
                    },
                    onNo = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            revert()
                            setEditable(inEdit)
                            ui.doButton.isEnabled = false
                            ui.doButton.animate().apply {
                                duration = 500
                                rotation(0f)
                            }.withEndAction {
                                ui.doButton.isEnabled = true
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
        }
    }
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.", ReplaceWith("super.onBackPressed()", "androidx.appcompat.app.AppCompatActivity"))
    override fun onBackPressed() {
        if(inEdit) {
            SignupActivity.showYesNoDialog(this, ContextCompat.getString(this, R.string.discard_changes),
                onYes = { super.onBackPressed() },
                onNo = {})
        } else super.onBackPressed()
    }
    private fun hint(message: String) { Pop.pop(this, message) }
}