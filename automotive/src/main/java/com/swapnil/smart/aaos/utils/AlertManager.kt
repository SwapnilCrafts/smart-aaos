package com.swapnil.smart.aaos.utils
data class VehicleAlert(
    val message: String,
    val severity: Severity,
    val dtcCode: String? = null
)
enum class Severity {
    LOW, MEDIUM, HIGH
}

