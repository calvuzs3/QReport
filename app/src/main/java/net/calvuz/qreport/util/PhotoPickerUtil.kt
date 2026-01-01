package net.calvuz.qreport.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Utility per gestire la selezione di foto dalla galleria.
 * Supporta Android 13+ Photo Picker API con fallback per versioni precedenti.
 */
object PhotoPickerUtil {

    /**
     * Intent per Android 13+ (Photo Picker)
     */
    private fun createPhotoPickerIntent(): Intent {
        return Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            type = "image/*"
            putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
        }
    }
}

/**
 * Launcher personalizzato per gestire la selezione foto con entrambe le API.
 */
@Composable
fun rememberPhotoPickerLauncher(
    onPhotoSelected: (Uri?) -> Unit
): PhotoPickerLauncher {

    // Android 13+ Photo Picker
    val modernPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onPhotoSelected(uri)
    }

    return remember {
        PhotoPickerLauncher(
            modernLauncher = modernPickerLauncher,
        )
    }
}

/**
 * Wrapper che gestisce entrambi i launcher in modo trasparente.
 */
class PhotoPickerLauncher(
    private val modernLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
) {
    /**
     * Lancia la selezione foto usando l'API appropriata per il dispositivo.
     */
    fun launch() {
            modernLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
    }
}

/**
 * Estensioni utility per gestire gli URI delle foto.
 */
object PhotoUriUtils {

    /**
     * Verifica se un URI è valido e accessibile.
     */
    fun isValidPhotoUri(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Ottiene il MIME type di un'immagine dall'URI.
     */
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }

    /**
     * Verifica se il MIME type è supportato per le foto.
     */
    fun isSupportedImageType(mimeType: String?): Boolean {
        return mimeType?.startsWith("image/") == true &&
                mimeType in listOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
        )
    }
}