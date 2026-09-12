package com.masuidrive.gestureime.conversion

/** A small, UI-independent boundary around the local Japanese conversion engine. */
interface ConversionEngine {
    suspend fun start(reading: String): ConversionState

    suspend fun update(reading: String): ConversionState

    suspend fun nextCandidate(): ConversionState

    suspend fun commit(index: Int): ConversionCommit?

    /** Returns local next-word candidates for text surrounding the editor cursor. */
    suspend fun predict(context: PredictionContext): ConversionState = ConversionState("", emptyList(), -1)

    /** Removes a selected Mozc history candidate, when the engine supports it. */
    suspend fun deleteCandidateFromHistory(index: Int): ConversionState? = null

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
    val source: ConversionCandidateSource = ConversionCandidateSource.MOZC,
)

/** Identifies which local store owns a conversion candidate. */
enum class ConversionCandidateSource { MOZC, ANDROID_USER_DICTIONARY }

data class ConversionCommit(val value: String)

/** Bounded editor text supplied to the local conversion engine for next-word prediction. */
data class PredictionContext(
    val precedingText: String,
    val followingText: String,
)
