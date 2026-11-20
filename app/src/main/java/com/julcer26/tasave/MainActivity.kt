package com.julcer26.tasave

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.MobileAds
import com.julcer26.tasave.ui.TasaVeScreen
import com.julcer26.tasave.ui.TasaVeViewModel
import com.julcer26.tasave.ui.theme.TasaVeTheme
import com.julcer26.tasave.widget.TasaVeUltraWidgetProvider

class MainActivity : ComponentActivity() {
    private var pendingWidgetAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingWidgetAction = intent?.getStringExtra(TasaVeUltraWidgetProvider.EXTRA_WIDGET_ACTION)

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
                    activity = this,
                    widgetAction = pendingWidgetAction,
                    onConsumeWidgetAction = { pendingWidgetAction = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetAction = intent.getStringExtra(TasaVeUltraWidgetProvider.EXTRA_WIDGET_ACTION)
    }
}
