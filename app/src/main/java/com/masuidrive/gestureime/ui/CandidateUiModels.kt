package com.masuidrive.gestureime.ui

enum class CandidatePresentation { SINGLE_LINE, VOICE }

data class CandidateUiSnapshot(
    val token: Long,
    val candidates: List<String>,
    val selectedIndex: Int = -1,
    val selectable: Boolean = true,
    val presentation: CandidatePresentation = CandidatePresentation.SINGLE_LINE,
)

data class CandidateUiEvent(
    val token: Long,
    val index: Int,
)

data class CandidateUiLongPressEvent(
    val token: Long,
    val index: Int,
)
