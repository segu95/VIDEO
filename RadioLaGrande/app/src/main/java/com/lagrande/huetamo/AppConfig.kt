package com.lagrande.huetamo

/**
 * Servidor de Radio La Grande de Huétamo.
 * El panel PHP debe instalarse en /radio dentro del hosting del dominio.
 */
object AppConfig {
    const val PANEL_BASE_URL = "https://lagrandedehuetamo.com/radio"
    const val API_URL = "$PANEL_BASE_URL/api.php"
    const val ADMIN_URL = "$PANEL_BASE_URL/admin.php"
}
