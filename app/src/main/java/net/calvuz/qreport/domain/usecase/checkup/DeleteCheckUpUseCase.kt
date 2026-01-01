package net.calvuz.qreport.domain.usecase.checkup

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import net.calvuz.qreport.domain.model.checkup.CheckUpStatus
import net.calvuz.qreport.domain.repository.CheckUpRepository
import net.calvuz.qreport.presentation.feature.checkup.model.CheckUpStatusExt.getDisplayName
import javax.inject.Inject

/**
 * AGGIORNATO: Non si possono eliminare check-up EXPORTED o ARCHIVED
 */
class DeleteCheckUpUseCase @Inject constructor(
    private val repository: CheckUpRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(checkUpId: String): Result<Unit> {
        return try {
            val checkUp = repository.getCheckUpById(checkUpId)
                ?: return Result.failure(Exception("CheckUp not found: $checkUpId"))

            // Validazione: non si possono eliminare check-up completati/esportati/archiviati
            if (checkUp.status in listOf(
                    CheckUpStatus.COMPLETED,
                    CheckUpStatus.EXPORTED,
                    CheckUpStatus.ARCHIVED
                )) {
                return Result.failure(
                    Exception("Cannot delete ${checkUp.status.getDisplayName(context)} check-up")
                )
            }

            repository.deleteCheckUp(checkUpId)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}