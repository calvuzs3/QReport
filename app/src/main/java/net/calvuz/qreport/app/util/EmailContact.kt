package net.calvuz.qreport.app.util

import android.content.Context
import android.content.Intent
import androidx.annotation.UiContext
import androidx.core.net.toUri
import timber.log.Timber

/**
 * Funzione per aprire il client email per scrivere una nuova email
 *
 * @param context Context Android (deve essere UiContext)
 * @param emailAddress Indirizzo email del destinatario
 * @return true se il client email è stato aperto con successo, false altrimenti
 */
fun emailContact(@UiContext context: Context, emailAddress: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            setData( "mailto:$emailAddress".toUri())
            // Opzionalmente puoi aggiungere:
            // putExtra(Intent.EXTRA_SUBJECT, "Oggetto email")
            // putExtra(Intent.EXTRA_TEXT, "Corpo email")
        }

        // Verifica se c'è un'app che può gestire l'intent
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        Timber.e(e, "Impossibile aprire il client email")
        false
    }
}

