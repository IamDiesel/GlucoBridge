package de.glucobridge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.glucobridge.app.ui.AppScreen
import de.glucobridge.app.ui.GlucoseViewModel
import de.glucobridge.app.ui.theme.GlucoBridgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GlucoBridgeApp).container
        setContent {
            GlucoBridgeTheme {
                val vm: GlucoseViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { GlucoseViewModel(container.glucoseRepository) }
                    }
                )
                AppScreen(vm)
            }
        }
    }
}
