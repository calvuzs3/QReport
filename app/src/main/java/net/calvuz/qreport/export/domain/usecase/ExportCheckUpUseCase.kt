package net.calvuz.qreport.export.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import net.calvuz.qreport.BuildConfig
import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.checkup.domain.model.CheckUpStatus
import net.calvuz.qreport.checkup.domain.repository.CheckUpRepository
import net.calvuz.qreport.checkup.domain.usecase.GetCheckUpDetailsUseCase
import net.calvuz.qreport.checkup.domain.usecase.UpdateCheckUpStatusUseCase
import net.calvuz.qreport.export.domain.reposirory.ExportData
import net.calvuz.qreport.export.domain.reposirory.ExportFormat
import net.calvuz.qreport.export.domain.reposirory.ExportOptions
import net.calvuz.qreport.export.domain.reposirory.ExportRepository
import net.calvuz.qreport.export.domain.reposirory.ExportTechnicalMetadata
import net.calvuz.qreport.export.domain.reposirory.MultiFormatExportResult
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case principale per export completo di checkup
 *
 * Responsabilità:
 * - Validazione business rules (status checkup)
 * - Coordinamento export multi-formato tramite repository
 * - Aggiornamento status checkup a EXPORTED
 * - Progress tracking per UI reattiva
 *
 * Sostituisce la logica precedentemente duplicata in ExportOptionsViewModel
 */
class ExportCheckUpUseCase @Inject constructor(
    private val checkUpRepository: CheckUpRepository,
    private val exportRepository: ExportRepository,
    private val getCheckUpDetailsUseCase: GetCheckUpDetailsUseCase,
    private val updateCheckUpStatusUseCase: UpdateCheckUpStatusUseCase
) {

    /**
     * Esegue export completo del checkup con tracking progresso
     *
     * @param checkUpId ID del checkup da esportare
     * @param options Configurazione export (formati, qualità foto, ecc.)
     * @return Flow con progresso export per UI reattiva
     */
    operator fun invoke(
        checkUpId: String,
        options: ExportOptions
    ): Flow<QrResult<MultiFormatExportResult, QrError>> = flow {

        try {
            Timber.Forest.i("Starting export for checkup: $checkUpId with formats: ${options.exportFormats}")

            // ===== 1. VALIDAZIONE BUSINESS RULES =====

            // Carica checkup
            val checkUp = checkUpRepository.getCheckUpById(checkUpId)
                ?: return@flow emit(QrResult.Error(QrError.Checkup.NOT_FOUND))

            // Valida status - business rule: solo checkup completati possono essere esportati
            if (!BuildConfig.DEBUG) {
                when (checkUp.status) {
                    CheckUpStatus.DRAFT, CheckUpStatus.IN_PROGRESS -> {
                        Timber.Forest.w("Cannot export checkup in status: ${checkUp.status}")
                        return@flow emit(QrResult.Error(QrError.Exporting.CANNOT_EXPORT_DRAFT))
                    }

                    CheckUpStatus.COMPLETED -> {
                        // OK - può essere esportato
                    }

                    CheckUpStatus.EXPORTED -> {
                        // OK - può essere ri-esportato
                        Timber.Forest.d("Re-exporting already exported checkup")
                    }

                    else -> {
                        // Altri status futuri
                    }
                }
            } else {
                Timber.w("[DEBUG] Skipping status validation")
            }

            // Valida opzioni export
            val validationErrors = options.validate()
            if (validationErrors.isNotEmpty()) {
                Timber.Forest.e("Export options validation failed: $validationErrors")
                return@flow emit(QrResult.Error(QrError.Checkup.FIELDS_REQUIRED))
            }

            // ===== 2. PREPARAZIONE DATI EXPORT =====

            // Carica dettagli completi checkup
            val checkUpDetails = when (val result = getCheckUpDetailsUseCase(checkUpId)) {
                is QrResult.Success -> result.data
                is QrResult.Error -> {
                    Timber.Forest.e("Failed to load checkup details for export")
                    return@flow emit(QrResult.Error(result.error))
                }
            }

            // Prepara dati per export usando modelli domain
            val exportData = ExportData(
                checkup = checkUpDetails.checkUp,
                itemsByModule = checkUpDetails.checkItems.groupBy { it.moduleType },
                statistics = checkUpDetails.statistics,
                progress = checkUpDetails.progress,
                exportMetadata = ExportTechnicalMetadata(
                    generatedAt = Clock.System.now(),
                    templateVersion = "1.0",
                    exportFormat = options.exportFormats.firstOrNull() ?: ExportFormat.WORD,
                    exportOptions = options
                )
            )

            Timber.Forest.i("Prepared export data: ${exportData.itemsByModule.size} modules, ${checkUpDetails.checkItems.size} items")

            // ===== 3. COORDINAMENTO EXPORT =====

            // Delega export al repository con progress tracking
            exportRepository.generateCompleteExport(exportData, options)
                .collect { exportResult ->

                    // Emetti progresso per UI
                    emit(QrResult.Success(exportResult))

                    // Se export completato con successo, aggiorna status
                    if (isExportCompleted(exportResult, options)) {
                        Timber.Forest.i("Export completed successfully, updating status to EXPORTED")

                        if (!BuildConfig.DEBUG) {
                            when (updateCheckUpStatusUseCase(checkUpId, CheckUpStatus.EXPORTED)) {
                                is QrResult.Success -> {
                                    Timber.Forest.d("Status updated to EXPORTED")
                                }

                                is QrResult.Error -> {
                                    Timber.Forest.w("Failed to update status to EXPORTED, but export succeeded")
                                    // Not a fatal error
                                }
                            }
                        } else {
                            Timber.w("[DEBUG] Skipping status update to EXPORTED")
                        }
                    }
                }

        } catch (e: Exception) {
            Timber.Forest.e(e, QrError.Checkup.EXPORT.name)
            emit(QrResult.Error(QrError.Checkup.EXPORT))
        }
    }

    /**
     * Verifica se l'export è completato per tutti i formati richiesti
     */
    private fun isExportCompleted(
        result: MultiFormatExportResult,
        options: ExportOptions
    ): Boolean {
        return options.exportFormats.all { format ->
            when (format) {
                ExportFormat.WORD -> result.wordResult != null
                ExportFormat.TEXT -> result.textResult != null
                ExportFormat.PHOTO_FOLDER -> result.photoFolderResult != null
                ExportFormat.COMBINED_PACKAGE ->
                    result.wordResult != null &&
                            result.textResult != null &&
                            result.photoFolderResult != null
            }
        }
    }
}