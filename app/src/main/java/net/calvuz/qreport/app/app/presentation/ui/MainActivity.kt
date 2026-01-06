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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.calvuz.qreport.app.navigation.QReportNavigation
import net.calvuz.qreport.app.app.presentation.ui.theme.QReportTheme
import timber.log.Timber

/**
 * MainActivity per QReport
 *
 * Activity principale dell'applicazione che gestisce:
 * - Dependency Injection con Hilt
 * - Navigation setup con Compose Navigation
 * - Theme configuration con Material Design 3
 * - Edge-to-edge layout per Android moderni
 *
 * @version 1.0
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("MainActivity onCreate - Starting QReport")

        // Enable edge-to-edge layout
        enableEdgeToEdge()

        setContent {
            QReportTheme {
                QReportApp()
            }
        }
    }
}

/**
 * Composable principale dell'app
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

@Preview(showBackground = true)
@Composable
fun QReportAppPreview() {
    QReportTheme {
        QReportApp()
    }
}