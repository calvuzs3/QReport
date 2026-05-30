package net.calvuz.qreport.client.client.presentation.model

data class ClientsStatistics (

    /**
     * Statistiche generali clienti
     */
        val activeClients: Int,
        val totalClients: Int,
        val inactiveClients: Int,
        val activationRate: Int, // Percentuale
)