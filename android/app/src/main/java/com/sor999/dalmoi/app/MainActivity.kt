package com.sor999.dalmoi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sor999.dalmoi.R
import com.sor999.dalmoi.presentation.common.component.Body
import com.sor999.dalmoi.presentation.common.component.label
import com.sor999.dalmoi.presentation.common.theme.DalmoiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DalmoiTheme {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Body(label(R.string.app_name))
                }
            }
        }
    }
}
