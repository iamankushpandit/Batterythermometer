package com.goodtime.thermometer.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.goodtime.thermometer.presentation.theme.ThermometerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keep screen on indefinitely
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            ThermometerTheme {
                ThermometerApp()
            }
        }
    }
}
