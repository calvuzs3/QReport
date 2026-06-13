package net.calvuz.qreport.client.contract.domain.model

import kotlinx.serialization.Serializable

/**
 * Customers' contracts detailed statistics
 */
@Serializable
class ContractStatistics(
    // ===== CONTATORI BASE =====
    val totalContracts: Int,
    val validContracts: Int,
    val outdatedContracts: Int,
) {


    companion object {
        /** Empty Statistics - error use
         */
        fun empty() = ContractStatistics(
            totalContracts = 0,
            validContracts = 0,
            outdatedContracts = 0,
        )
    }
}