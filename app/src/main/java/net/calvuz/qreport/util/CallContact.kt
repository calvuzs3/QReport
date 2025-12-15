package net.calvuz.qreport.util

import android.content.Intent
import androidx.annotation.UiContext
import androidx.core.net.toUri
import timber.log.Timber


// Funzione per chiamare contatto
fun callContact(@UiContext context: android.content.Context, phoneNumber: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$phoneNumber".toUri()
        }

        // Verifica se c'è un'app che può gestire l'intent
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        Timber.e("Impossibile effettuare la chiamata telefonica")
        false
    }
}
