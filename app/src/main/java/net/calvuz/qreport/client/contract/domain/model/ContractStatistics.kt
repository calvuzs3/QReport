package net.calvuz.qreport.client.contract.domain.model

import kotlinx.serialization.Serializable

/**
 * Customers' contracts detailed statistics
 */
@Serializable
class ContractStatistics(
    // ===== CONTATORI BASE =====
    val totalContracts: Int,
    val activeContracts: Int,
    val inactiveContracts: Int,
) {


    companion object {
        /** Empty Statistics - error use
         */
        fun empty() = ContractStatistics(
            totalContracts = 0,
            activeContracts = 0,
            inactiveContracts = 0,
        )
    }
}