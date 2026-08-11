package com.example.surymeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.surymeter.ui.HomeScreen
import com.example.surymeter.ui.theme.SuryMeterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuryMeterTheme {
                HomeScreen()
            }
        }
    }
}
