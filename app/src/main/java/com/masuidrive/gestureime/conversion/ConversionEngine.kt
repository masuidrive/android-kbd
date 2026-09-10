package com.masuidrive.gestureime.conversion

/** A small, UI-independent boundary around the local Japanese conversion engine. */
interface ConversionEngine {
    suspend fun start(reading: String): ConversionState

    suspend fun update(reading: String): ConversionState

    suspend fun nextCandidate(): ConversionState

    suspend fun commit(index: Int): ConversionCommit?

    suspend fun reset()
}

data class ConversionState(
    val reading: String,
    val candidates: List<ConversionCandidate>,
    val selectedIndex: Int,
)

data class ConversionCandidate(
    val id: Int,
    val value: String,
)

data class ConversionCommit(val value: String)
