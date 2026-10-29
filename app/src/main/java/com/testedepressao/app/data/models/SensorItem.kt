package com.testedepressao.app.data.models

enum class SensorCategory(val displayName: String) {
    TODOS("Todos"),
    MOTOR("Motor"),
    TEMPERATURAS("Temperaturas"),
    ELETRICA("Elétrica"),
    DINAMICA("Dinâmica")
}

data class SensorItem(
    val id: String,
    val name: String,
    val pid: String,
    val category: SensorCategory,
    val unit: String,
    val value: String = "--",
    val numericValue: Float = 0.0f,
    val isEnabled: Boolean = true,
    val description: String = ""
)
