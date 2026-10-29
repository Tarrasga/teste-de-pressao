package com.testedepressao.app.data.models

enum class BatteryCondition(
    val title: String,
    val description: String,
    val isGood: Boolean
) {
    EXCELENTE(
        "Excelente (100%)",
        "A bateria tem plena capacidade de carga e resposta rápida no arranque. Start & Stop 100% operacional.",
        true
    ),
    BOA(
        "Boa (~75% a 85%)",
        "Bateria saudável com tensão estável. Adequada para o uso diário normal da carrinha.",
        true
    ),
    FRACA(
        "Fraca / Degradação Detetada",
        "Tensão baixa de repouso ou queda excessiva no arranque. É a causa principal de a 508 desativar o Start & Stop e ativar o 'Modo Economia de Energia' prematuramente.",
        false
    ),
    CRITICA(
        "Crítica / Substituição Urgente",
        "Tensão perigosamente baixa. Risco iminente de a carrinha não pegar num arranque a frio.",
        false
    )
}

data class BatteryHealthData(
    val liveVoltage: Float = 12.4f,
    val restingVoltage: Float? = null,
    val minCrankVoltage: Float? = null,
    val chargingVoltage: Float? = null,
    val estimatedSocPercent: Int = 75,
    val isAlternatorCharging: Boolean = false,
    val isEngineRunning: Boolean = false,
    val condition: BatteryCondition = BatteryCondition.BOA,
    val recommendation: String = "Ligue a ignição com motor desligado para medir a tensão de repouso."
)
