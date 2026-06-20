package net.calvuz.qreport.checkup.checkup.domain.model.spare

/**
 * Categorie dei ricambi
 */
enum class SparePartCategory (val displayName: String, val displayIcon: String) {
    MECHANICAL("Meccanici", "🔧"),       // Meccanici
    ELECTRICAL("Elettrici", "⚡"),        // Elettrici
    PNEUMATIC("Pneumatici", "💨"),       // Pneumatici
    HYDRAULIC("Idraulici", "🌊"),        // Idraulici
    ELECTRONIC("Elettronici", "🔌"),      // Elettronici
    SOFTWARE("Software", "💻"),          // Software
    CONSUMABLE("Consumabili", "📦"),     // Materiali di consumo
    TOOL("Utensili", "🛠️"),              // Utensili
    OTHER("Altri", "📋");                 // Altri

}