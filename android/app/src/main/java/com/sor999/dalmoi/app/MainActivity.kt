package com.sor999.dalmoi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sor999.dalmoi.navigation.DalmoiApp
import com.sor999.dalmoi.presentation.common.theme.DalmoiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DalmoiTheme { DalmoiApp() } }
    }
}
