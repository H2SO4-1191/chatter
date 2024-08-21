package com.h2so4.chatter

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View.OnAttachStateChangeListener
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.h2so4.chatter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var ui: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setDrawer()
        ui.mainContent.password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                ui.mainContent.loginDoor.isVisible = true
                ui.mainContent.loginDoor.animate().apply {
                    duration = 500
                    translationX(400f)
                }.start()
                ui.mainContent.email.animate().apply {
                    duration = 500
                    translationY(-100f)
                }.start()
                ui.mainContent.password.animate().apply {
                    duration = 500
                    translationY(100f)
                }.start()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        ui.mainContent.loginDoor.setOnClickListener {
            ui.mainContent.loginDoor.animate().apply {
                duration = 500
                translationX(1000f)
            }.start()
            ui.mainContent.email.animate().apply {
                duration = 500
                translationY(-1000f)
            }.start()
            ui.mainContent.password.animate().apply {
                duration = 500
                translationY(1000f)
            }.withEndAction {
                ui.mainContent.email.isVisible = false
                ui.mainContent.password.isVisible = false
                ui.mainContent.loginDoor.isVisible = false
            }

        }
    }

    private fun setDrawer() {
        drawerLayout = ui.drawerLayout
        navigationView = ui.navigationDrawer
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            Toast.makeText(this, menuItem.title, Toast.LENGTH_SHORT).show()
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }
    override fun onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
        super.onBackPressed()
    }
}