package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.TasaVeScreen
import com.example.ui.TasaVeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Retrieve our custom currency Cotizador and Calculator ViewModel
        val viewModel = ViewModelProvider(this)[TasaVeViewModel::class.java]
        
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = themeMode == "DARK"
            
            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                TasaVeScreen(viewModel = viewModel)
            }
        }
    }
}
