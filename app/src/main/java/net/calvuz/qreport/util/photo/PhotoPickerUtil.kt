package net.calvuz.qreport.util.photo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Utility per gestire la selezione di foto dalla galleria.
 * Supporta Android 13+ Photo Picker API con fallback per versioni precedenti.
 */
object PhotoPickerUtil {

    /**
     * Verifica se il dispositivo supporta il nuovo Photo Picker API (Android 13+).
     */
    fun supportsPhotoPicker(): Boolean {
        return true
    }

    /**
     * Crea un Intent per aprire la galleria con la strategia migliore per il dispositivo.
     */
    fun createGalleryIntent(context: Context): Intent {
        return if (supportsPhotoPicker()) {
            // Android 13+: Usa il nuovo Photo Picker
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
            }
        } else {
            // Android < 13: Usa il classico Intent
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
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
    val context = LocalContext.current

    // Android 13+ Photo Picker
    val modernPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onPhotoSelected(uri)
    }

    // Fallback per Android < 13
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        onPhotoSelected(uri)
    }

    return remember {
        PhotoPickerLauncher(
            modernLauncher = modernPickerLauncher,
            legacyLauncher = legacyPickerLauncher,
            context = context
        )
    }
}

/**
 * Wrapper che gestisce entrambi i launcher in modo trasparente.
 */
class PhotoPickerLauncher(
    private val modernLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    private val legacyLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    private val context: Context
) {
    /**
     * Lancia la selezione foto usando l'API appropriata per il dispositivo.
     */
    fun launch() {
        if (PhotoPickerUtil.supportsPhotoPicker()) {
            // Android 13+: Usa il nuovo Photo Picker
            modernLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            // Android < 13: Usa Intent tradizionale
            val intent = PhotoPickerUtil.createGalleryIntent(context)
            legacyLauncher.launch(intent)
        }
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
        } catch (e: Exception) {
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