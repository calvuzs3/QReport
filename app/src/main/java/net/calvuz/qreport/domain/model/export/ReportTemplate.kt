package net.calvuz.qreport.domain.model.export

/**
 * Template semplificato per la formattazione del report
 * Mantiene solo le configurazioni essenziali
 */
data class ReportTemplate(
    /**
     * Nome identificativo del template
     */
    val name: String,

    /**
     * Colore principale per header (formato HEX senza #)
     */
    val headerColor: String = "2E7D32", // Verde corporate

    /**
     * Path al logo aziendale
     */
    val logoPath: String = "",

    /**
     * Testo per footer del documento
     */
    val footerText: String = "Report generato da QReport",

    /**
     * Font family
     */
    val fontFamily: String = "Calibri",

    /**
     * Dimensione font base
     */
    val baseFontSize: Int = 11
) {
    companion object {
        /**
         * Template di default aziendale
         */
        fun default() = ReportTemplate(
            name = "QReport Minimal",
            headerColor = "333333",
            logoPath = "",
            footerText = "Report generato da QReport v1.0"
        )

        /**
         * Template minimale per test
         */
        fun minimal() = ReportTemplate(
            name = "Standard",
            headerColor = "2E7D32",
            logoPath = "/assets/qreport_logo.png",
            footerText = "QReport"
        )
    }
}