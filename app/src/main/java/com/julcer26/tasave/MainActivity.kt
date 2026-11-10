package com.julcer26.tasave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.MobileAds
import com.julcer26.tasave.ui.TasaVeScreen
import com.julcer26.tasave.ui.TasaVeViewModel
import com.julcer26.tasave.ui.theme.TasaVeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads (AdMob) in the background
        MobileAds.initialize(this) {}

        // Retrieve ViewModel with SQLite Room, DolarAPI & Google Play Billing integrations
        val viewModel = ViewModelProvider(this)[TasaVeViewModel::class.java]

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = themeMode == "DARK"

            TasaVeTheme(darkTheme = isDark, dynamicColor = false) {
                TasaVeScreen(
                    viewModel = viewModel,
                    activity = this
                )
            }
        }
    }
}
