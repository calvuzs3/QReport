package net.calvuz.qreport.app.app.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.calvuz.qreport.app.navigation.QReportNavigation
import net.calvuz.qreport.app.app.presentation.ui.theme.QReportTheme
import timber.log.Timber

/**
 * MainActivity
 *
 * QReport's Main Activity:
 * - Hilt dependency injection
 * - Navigation setup
 * - Theme configuration
 * - Edge-to-edge layout
 *
 * @version 1.0
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("Starting QReport")

        // Enable edge-to-edge layout
        enableEdgeToEdge()

        // Abilita edge-to-edge per gestire correttamente gli insets
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            QReportTheme {
                QReportApp()
            }
        }
    }
}

/**
 * Main Composable
 */
@Composable
fun QReportApp() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            QReportNavigation(
                navController = navController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}