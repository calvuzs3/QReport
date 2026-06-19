package net.calvuz.qreport.checkup.domain.repository

import net.calvuz.qreport.checkup.domain.model.CheckItemTemplate

/**
 * Repository per i template di controllo
 */

interface CheckItemTemplateRepository {

    suspend fun getAllTemplates(): List<CheckItemTemplate>

    suspend fun getTemplatesForIslandType(islandType: String): List<CheckItemTemplate>

    suspend fun getTemplatesByModule(moduleType: String): List<CheckItemTemplate>

    suspend fun initializeDefaultTemplates()

    suspend fun addTemplate(template: CheckItemTemplate)

    suspend fun updateTemplate(template: CheckItemTemplate)

    suspend fun deleteTemplate(id: String)
}