package net.calvuz.qreport

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * CUSTOM TEST RUNNER PER HILT INTEGRATION
 *
 * Necessario per Hilt Android testing con instrumented tests.
 * Sostituisce l'Application normale con HiltTestApplication durante test.
 */
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}