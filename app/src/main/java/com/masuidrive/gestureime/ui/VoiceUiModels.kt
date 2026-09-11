package com.masuidrive.gestureime.ui

sealed interface VoiceUiState {
    data object Hidden : VoiceUiState
    data object Idle : VoiceUiState
    data object Recording : VoiceUiState
    data object Recognizing : VoiceUiState
    data class Preview(val text: String) : VoiceUiState
    data class Unavailable(val message: String) : VoiceUiState
    data object PermissionRequired : VoiceUiState
}

enum class VoiceUiAction { Cancel, RequestPermission, ExplainUnavailable }

data class VoiceUiSnapshot(val sessionToken: Long, val state: VoiceUiState)

data class VoiceUiEvent(val sessionToken: Long, val action: VoiceUiAction)
