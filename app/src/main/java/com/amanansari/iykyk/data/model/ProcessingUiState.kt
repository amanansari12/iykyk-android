package com.amanansari.iykyk.data.model

data class ProcessingUiState(
    val phase: ProcessingPhase? = null,
    val progress: Float = 0f,
    val message: String = "",
    val isProcessing: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null,

    // New failure state
    val isProcessFailed: Boolean = false,
    val failureCountdown: Int = 0
)