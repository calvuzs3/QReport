package net.calvuz.qreport.app.util

import android.content.Context
import android.content.Intent
import androidx.annotation.UiContext
import androidx.core.net.toUri
import timber.log.Timber


// Funzione per chiamare contatto
fun callContact(@UiContext context: Context, phoneNumber: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            setData( "tel:$phoneNumber".toUri())
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
