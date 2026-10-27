package com.masuidrive.gestureime

import android.content.res.XmlResourceParser
import android.os.Build
import androidx.autofill.inline.UiVersions
import androidx.autofill.inline.v1.InlineSuggestionUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InlineSuggestionsTest {
    @Test
    fun inlineSuggestionsAreGuardedToAndroid11AndLater() {
        assertFalse(supportsInlineSuggestions(Build.VERSION_CODES.P))
        assertFalse(supportsInlineSuggestions(Build.VERSION_CODES.Q))
        assertTrue(supportsInlineSuggestions(Build.VERSION_CODES.R))
    }

    @Test
    @Config(sdk = [28])
    fun preAndroid11RuntimeDoesNotLoadOrEnableInlineSuggestions() {
        assertFalse(supportsInlineSuggestions())
    }

    @Test
    fun staleInflationCallbackIsRejectedAfterEditorLifecycleClearsInlineContent() {
        val controller = InlineSuggestionController()
        val firstResponse = controller.beginResponse()
        assertTrue(controller.isActive)
        assertTrue(controller.accepts(firstResponse))

        controller.clear()

        assertFalse(controller.isActive)
        assertFalse(controller.accepts(firstResponse))
        val nextResponse = controller.beginResponse()
        assertTrue(controller.accepts(nextResponse))
        assertFalse(controller.accepts(firstResponse))
    }

    @Test
    fun requestUsesCandidateStripHeightAndBoundedProviderViewWidth() {
        val density = 2f
        val request = createInlineSuggestionsRequest(density, supportedUiExtras())!!
        val presentation = request.inlinePresentationSpecs.single()

        assertEquals(INLINE_SUGGESTION_MAX_COUNT, request.maxSuggestionCount)
        assertEquals(INLINE_SUGGESTION_MIN_WIDTH_DP * density, presentation.minSize.width.toFloat(), 0f)
        assertEquals(INLINE_SUGGESTION_MAX_WIDTH_DP * density, presentation.maxSize.width.toFloat(), 0f)
        assertEquals(INLINE_SUGGESTION_HEIGHT_DP * density, presentation.minSize.height.toFloat(), 0f)
        assertEquals(INLINE_SUGGESTION_HEIGHT_DP * density, presentation.maxSize.height.toFloat(), 0f)
        assertEquals(presentation.maxSize, inlineSuggestionInflationSize(density))
        assertTrue(UiVersions.INLINE_UI_VERSION_1 in UiVersions.getVersions(presentation.style))
    }

    @Test
    fun requestIsNotCreatedWhenPlatformDoesNotOfferTheSupportedUiTemplate() {
        assertEquals(null, createInlineSuggestionsRequest(1f, android.os.Bundle()))
    }

    @Test
    fun inputMethodMetadataAdvertisesInlineSuggestionSupport() {
        val parser = org.robolectric.RuntimeEnvironment.getApplication().resources.getXml(R.xml.method)
        while (parser.eventType != XmlResourceParser.START_TAG) parser.next()

        assertTrue(parser.getAttributeBooleanValue(
            "http://schemas.android.com/apk/res/android",
            "supportsInlineSuggestions",
            false,
        ))
        parser.close()
    }

    private fun supportedUiExtras() = UiVersions.newStylesBuilder()
        .addStyle(InlineSuggestionUi.newStyleBuilder().build())
        .build()
}
