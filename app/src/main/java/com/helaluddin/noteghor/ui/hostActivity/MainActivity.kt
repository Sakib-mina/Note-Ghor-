package com.helaluddin.noteghor.ui.hostActivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.helaluddin.noteghor.data.utils.ThemeHelper
import com.helaluddin.noteghor.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {

        // -------------------------------
        // Apply saved theme BEFORE super.onCreate
        // -------------------------------
        ThemeHelper.applyTheme(ThemeHelper.getSavedTheme(this), this)

        // Splash screen install
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Inflate view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep splash screen until app is ready
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        // Simulate initialization (like loading data, notes, etc.)
        lifecycleScope.launch {
            delay(1000) // short delay for splash
            isAppReady = true
        }
    }
}
