package com.masuidrive.gestureime.ui

data class CandidateUiSnapshot(
    val token: Long,
    val candidates: List<String>,
    val selectedIndex: Int = -1,
    val selectable: Boolean = true,
)

data class CandidateUiEvent(
    val token: Long,
    val index: Int,
)

data class CandidateUiLongPressEvent(
    val token: Long,
    val index: Int,
)
