package net.calvuz.qreport.client.client.domain.model

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