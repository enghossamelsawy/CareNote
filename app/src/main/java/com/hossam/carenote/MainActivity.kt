package com.hossam.carenote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.navigation.CareNoteNavDisplay
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set before anything is drawn: screenshots, recordings and the recent-apps preview come out blank.
        if (BuildConfig.SECURE_SCREEN) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        enableEdgeToEdge()
        setContent {
            CareNoteTheme {
                CareNoteNavDisplay()
            }
        }
    }
}
