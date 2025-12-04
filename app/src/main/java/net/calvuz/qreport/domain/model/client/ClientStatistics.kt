package net.calvuz.qreport.domain.model.client

data class ClientStatistics (

    /**
     * Statistiche generali clienti
     */
        val activeClients: Int,
        val totalClients: Int,
        val inactiveClients: Int,
        val activationRate: Int, // Percentuale
        val totalIndustries: Int,
        val industries: List<String>
)