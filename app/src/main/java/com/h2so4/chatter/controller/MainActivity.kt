package com.h2so4.chatter.controller

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.h2so4.chatter.R
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
        setChatterPressed()
    }

    private fun setDrawer() {
        drawerLayout = ui.drawerLayout
        navigationView = ui.navigationDrawer
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navigationView.bringToFront()
        navigationView.setNavigationItemSelectedListener { menuItem ->
            Toast.makeText(this, menuItem.title, Toast.LENGTH_SHORT).show()
            false
        }
    }
    private fun setChatterPressed() {
        ui.mainContent.chatter.setOnClickListener {
            ui.mainContent.chatter.animate().apply {
                duration = 2000
                rotationYBy(1440f)
            }.withEndAction {
                val signingIntent = Intent(this, EnteringActivity::class.java)
                startActivity(signingIntent)
            }.start()
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