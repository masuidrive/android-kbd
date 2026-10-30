package com.masuidrive.gestureime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.inline.InlinePresentationSpec
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.common.TextViewStyle
import androidx.autofill.inline.common.ViewStyle
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
@SuppressLint("RestrictedApi") // AndroidX v1 exposes ViewStyle through a library-restricted base builder.
internal fun createInlineSuggestionsRequest(
    context: Context,
    @Suppress("UNUSED_PARAMETER") uiExtras: Bundle,
): InlineSuggestionsRequest {
    val density = context.resources.displayMetrics.density
    // Android 11's system request can have empty uiExtras. This IME declares the v1 template in
    // its own presentation style, which lets compatible Autofill providers render their views.
    fun size(widthDp: Int) = Size((widthDp * density).toInt(), (INLINE_SUGGESTION_HEIGHT_DP * density).toInt())
    val chipStyle = ViewStyle.Builder()
        .setBackground(Icon.createWithResource(context, R.color.candidate_background))
        .setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
        .build()
    val style = InlineSuggestionUi.newStyleBuilder()
        .setChipStyle(chipStyle)
        .setSingleIconChipStyle(chipStyle)
        .setTitleStyle(TextViewStyle.Builder()
            .setTextColor(context.getColor(R.color.keyboard_text))
            .setTextSize(15f)
            .build())
        .setSubtitleStyle(TextViewStyle.Builder()
            .setTextColor(context.getColor(R.color.keyboard_muted_text))
            .setTextSize(13f)
            .build())
        .build()
    val presentation = InlinePresentationSpec.Builder(
        size(INLINE_SUGGESTION_MIN_WIDTH_DP),
        size(INLINE_SUGGESTION_MAX_WIDTH_DP),
    ).setStyle(
        UiVersions.newStylesBuilder()
            .addStyle(style)
            .build(),
    ).build()
    return InlineSuggestionsRequest.Builder(listOf(presentation))
        .setMaxSuggestionCount(INLINE_SUGGESTION_MAX_COUNT)
        .build()
}

internal fun inlineSuggestionInflationSize(
    density: Float,
    widthDp: Int = INLINE_SUGGESTION_MAX_WIDTH_DP,
): Size = Size(
    (widthDp.coerceIn(INLINE_SUGGESTION_MIN_WIDTH_DP, INLINE_SUGGESTION_MAX_WIDTH_DP) * density).toInt(),
    (INLINE_SUGGESTION_HEIGHT_DP * density).toInt(),
)

internal fun shouldRestoreCandidatesAfterInlineInflation(
    pendingInflations: Int,
    renderedViews: Int,
): Boolean = pendingInflations == 0 && renderedViews == 0
