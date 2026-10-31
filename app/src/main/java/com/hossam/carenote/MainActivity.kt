package com.hossam.carenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hossam.carenote.core.designsystem.theme.CareNoteTheme
import com.hossam.carenote.feature.residents.ResidentsRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CareNoteTheme {
                // TODO(2.3): replace with the Navigation 3 host.
                ResidentsRoute(onOpenResident = {})
            }
        }
    }
}
