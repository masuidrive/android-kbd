package com.masuidrive.gestureime

import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.inline.InlinePresentationSpec
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi

/** Tracks the lifetime of protected InlineSuggestion views independently from text candidates. */
internal class InlineSuggestionController {
    private var generation = 0L

    var isActive: Boolean = false
        private set

    fun beginResponse(): Long {
        isActive = true
        return ++generation
    }

    fun clear() {
        isActive = false
        generation++
    }

    fun accepts(callbackGeneration: Long): Boolean = isActive && callbackGeneration == generation
}

internal fun supportsInlineSuggestions(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.R

internal const val INLINE_SUGGESTION_MIN_WIDTH_DP = 82
internal const val INLINE_SUGGESTION_MAX_WIDTH_DP = 360
internal const val INLINE_SUGGESTION_HEIGHT_DP = 38
internal const val INLINE_SUGGESTION_MAX_COUNT = 5

@androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
internal fun createInlineSuggestionsRequest(density: Float, uiExtras: Bundle): InlineSuggestionsRequest? {
    if (UiVersions.INLINE_UI_VERSION_1 !in UiVersions.getVersions(uiExtras)) return null
    fun size(widthDp: Int) = Size((widthDp * density).toInt(), (INLINE_SUGGESTION_HEIGHT_DP * density).toInt())
    val presentation = InlinePresentationSpec.Builder(
        size(INLINE_SUGGESTION_MIN_WIDTH_DP),
        size(INLINE_SUGGESTION_MAX_WIDTH_DP),
    ).setStyle(
        UiVersions.newStylesBuilder()
            .addStyle(InlineSuggestionUi.newStyleBuilder().build())
            .build(),
    ).build()
    return InlineSuggestionsRequest.Builder(listOf(presentation))
        .setMaxSuggestionCount(INLINE_SUGGESTION_MAX_COUNT)
        .build()
}

internal fun inlineSuggestionInflationSize(density: Float): Size = Size(
    (INLINE_SUGGESTION_MAX_WIDTH_DP * density).toInt(),
    (INLINE_SUGGESTION_HEIGHT_DP * density).toInt(),
)
