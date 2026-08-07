package com.brailuxaprende

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.brailuxaprende.ui.navigation.BrailuxApp
import com.brailuxaprende.ui.theme.BrailuxAprendeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrailuxAprendeTheme {
                BrailuxApp()
            }
        }
    }
}
