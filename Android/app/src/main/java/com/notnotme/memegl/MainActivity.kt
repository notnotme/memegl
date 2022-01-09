/*
 * Meme Présidents, swap a président face with yours.
 * Copyright (C) 2022  Romain Graillot
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.notnotme.memegl

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.notnotme.memegl.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        const val TAG = "MainActivity"
    }

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.bind(findViewById(R.id.layout))
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            // Fix older device navigation bar style
            window.navigationBarColor = Color.BLACK
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_share_app -> {
            shareApplication(); true
        }
        R.id.action_googleplay_sheet -> {
            googlePlaySheet(); true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp() = findNavController(R.id.nav_host_fragment).run {
        navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun shareApplication() {
        try {
            ShareCompat.IntentBuilder(this)
                .setType("text/plain")
                .setText(getString(R.string.share_app_text))
                .startChooser()
        } catch (exception: Exception) {
            Log.e(TAG, "Error: ${exception.message}")
        }
    }

    private fun googlePlaySheet() {
        try {
            // Google Play app may be missing.
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Error opening GooglePlay: ${e.message}, using web link instead.")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

}
