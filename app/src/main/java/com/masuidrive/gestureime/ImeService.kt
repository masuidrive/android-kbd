package com.masuidrive.gestureime

import android.content.ClipboardManager
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.util.Consumer
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.RecentEmojiProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.masuidrive.gestureime.conversion.ConversionCandidate
import com.masuidrive.gestureime.conversion.ConversionCandidateSource
import com.masuidrive.gestureime.conversion.ConversionEngine
import com.masuidrive.gestureime.conversion.ConversionState
import com.masuidrive.gestureime.conversion.MozcConversionEngine
import com.masuidrive.gestureime.keyboard.KeyAction
import com.masuidrive.gestureime.keyboard.KeyboardActionSink
import com.masuidrive.gestureime.keyboard.KeyboardMode
import com.masuidrive.gestureime.keyboard.KeyboardView
import com.masuidrive.gestureime.keyboard.VoiceHoldEvent
import com.masuidrive.gestureime.keyboard.VoiceHoldSink
import com.masuidrive.gestureime.suggestion.BundledEnglishSuggestionEngine
import com.masuidrive.gestureime.suggestion.EnglishSuggestionEngine
import com.masuidrive.gestureime.ui.CandidateUiEvent
import com.masuidrive.gestureime.ui.CandidateUiLongPressEvent
import com.masuidrive.gestureime.ui.CandidateUiSnapshot
import com.masuidrive.gestureime.ui.CandidateStripView
import com.masuidrive.gestureime.ui.VoiceUiSnapshot
import com.masuidrive.gestureime.ui.VoiceUiState
import com.masuidrive.gestureime.ui.VoicePanelView
import com.masuidrive.gestureime.voice.VoiceBackendState
import com.masuidrive.gestureime.voice.VoiceRecognitionController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class ImeService : InputMethodService(), KeyboardActionSink, VoiceHoldSink {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val actionMutex = Mutex()
    private val editorSession = EditorSessionGate()
    private val productionConversionEngine: ConversionEngine by lazy { MozcConversionEngine(applicationContext) }
    private var testConversionEngine: ConversionEngine? = null
    private val conversionEngine: ConversionEngine get() = testConversionEngine ?: productionConversionEngine
    private val productionEnglishSuggestionEngine: EnglishSuggestionEngine by lazy {
        BundledEnglishSuggestionEngine(applicationContext)
    }
    private var testEnglishSuggestionEngine: EnglishSuggestionEngine? = null
    private val englishSuggestionEngine: EnglishSuggestionEngine
        get() = testEnglishSuggestionEngine ?: productionEnglishSuggestionEngine
    private lateinit var textController: TextInputController
    private lateinit var voiceController: VoiceRecognitionController
    private var keyboardView: KeyboardView? = null
    private var candidateStrip: CandidateStripView? = null
    private var voicePanel: VoicePanelView? = null
    private var publicEmojiPicker: EmojiPickerView? = null
    private var privateEmojiPicker: EmojiPickerView? = null
    private var emojiPickerBottomMask: View? = null
    private val pickerLayoutSignatures = mutableMapOf<EmojiPickerView, Pair<Int, Int>>()
    private val pickerViewportHeights = mutableMapOf<EmojiPickerView, Int>()
    private val pickerViewportMaximums = mutableMapOf<EmojiPickerView, Int>()
    private val pickerViewportLocked = mutableSetOf<EmojiPickerView>()
    private val pickerViewportCategoryTransitions = mutableMapOf<EmojiPickerView, EmojiCategoryTransition>()
    private var emojiCategoryTransitionGeneration = 0L
    private val pickerBodies = mutableMapOf<EmojiPickerView, RecyclerView>()
    /** Bodies which have received AndroidX's one-time provisional cell measurement. */
    private val pickerBodyOverscanPrepared = mutableSetOf<RecyclerView>()
    /** Prepared bodies waiting for their first attached cell before final-height restoration. */
    private val pickerBodyOverscanPending = mutableSetOf<RecyclerView>()
    private val pickerHeaders = mutableMapOf<EmojiPickerView, RecyclerView>()
    private var emojiPickerHeaderHeight = 0
    private var emojiPickerControlTop = 0
    private var conversionGeneration = 0L
    private var reading = ""
    private var candidates = emptyList<String>()
    private var conversionCandidates = emptyList<ConversionCandidate>()
    private var selectedCandidate = -1
    private var conversionPreview: String? = null
    private var candidateSource = CandidateSource.NONE
    private var englishBuffer = ""
    private var englishGeneration = 0L
    private var slashBufferActive = false
    private var slashGeneration = 0L
    private var predictionGeneration = 0L
    private var pendingPredictionSelection: ExpectedSelectionTransition? = null
    private var predictionRequestInFlight = false
    private var keyboardMode = KeyboardMode.QWERTY
    private var voiceReturnMode = KeyboardMode.QWERTY
    private var candidateUiToken = 0L
    private var voiceUiToken = 0L
    private var voiceHoldRequestId: Long? = null
    private var voiceHoldEditorToken = 0L
    private var voiceHoldReady = false
    private var voiceHoldReleased = false
    private var voiceHoldGeneration = 0L
    /** Invalidates queued voice-candidate commits when the dedicated layer leaves. */
    private var voiceSessionGeneration = 0L

    override fun onCreate() {
        super.onCreate()
        textController = TextInputController(
            connection = { currentInputConnection },
            context = applicationContext,
            clipboard = getSystemService(ClipboardManager::class.java),
        )
        voiceController = VoiceRecognitionController(applicationContext, ::onVoiceState)
        // The picker default provider is asynchronous and may retain an old selection. Our
        // providers own all recents, so clear the library store before either picker exists.
        getSharedPreferences("androidx.emoji2.emojipicker.preferences", MODE_PRIVATE).edit().clear().commit()
    }

    override fun onCreateInputView(): View {
        if (keyboardMode == KeyboardMode.VOICE) cancelVoiceSession() else cancelVoiceHold()
        // A fresh input view owns fresh pickers; do not retain its detached predecessors.
        pickerLayoutSignatures.clear()
        pickerViewportHeights.clear()
        pickerViewportMaximums.clear()
        pickerViewportLocked.clear()
        pickerViewportCategoryTransitions.clear()
        pickerBodies.clear()
        pickerBodyOverscanPrepared.clear()
        pickerBodyOverscanPending.clear()
        pickerHeaders.clear()
        val candidateHeight = (50 * resources.displayMetrics.density).toInt()
        val keyboard = KeyboardView(this).also {
            it.actionSink = this
            it.voiceHoldSink = this
            // The navigation area belongs below the four key rows. Seed it before the input
            // view's first measure; otherwise a later candidate layout is the first chance to
            // add it and the whole IME jumps upward.
            it.updateBottomInset(initialKeyboardBottomInset())
            keyboardMode = ImePreferences.getLastKeyboardMode(this)
            it.setMode(keyboardMode)
            it.setEmojiRecents(ImePreferences.getEmojiRecents(this))
            it.setDualFlickEnabled(ImePreferences.isDualFlickEnabled(this))
            it.setHeightPreset(ImePreferences.getKeyboardHeightPreset(this))
            keyboardView = it
        }
        val strip = CandidateStripView(this).also {
            it.setOnCandidateSelected(::onCandidateSelected)
            it.setOnCandidateLongPressed(::onCandidateLongPressed)
            it.visibility = if (textController.isPrivateField) View.INVISIBLE else View.VISIBLE
            candidateStrip = it
        }
        val initialPickerViewportHeight = keyboard.emojiPickerOverlayHeight().toInt()
        emojiPickerHeaderHeight = candidateHeight
        emojiPickerControlTop = candidateHeight + initialPickerViewportHeight
        // The picker root and its RecyclerView body end exactly at the fixed control row.
        val initialPickerHeight = candidateHeight + initialPickerViewportHeight
        val publicPicker = createEmojiPicker(publicRecentProvider()).also { publicEmojiPicker = it }
        val privatePicker = createEmojiPicker(privateRecentProvider()).also { privateEmojiPicker = it }
        pickerViewportHeights[publicPicker] = initialPickerViewportHeight
        pickerViewportHeights[privatePicker] = initialPickerViewportHeight
        pickerViewportMaximums[publicPicker] = initialPickerViewportHeight
        pickerViewportMaximums[privatePicker] = initialPickerViewportHeight
        val pickerBottomMask = View(this).apply {
            id = R.id.emoji_picker_bottom_mask
            setBackgroundColor(getColor(R.color.keyboard_background))
            isClickable = true
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            visibility = View.GONE
            emojiPickerBottomMask = this
        }
        val panel = VoicePanelView(this).also {
            it.id = R.id.voice_panel
            it.setOnCandidateSelected(::onCandidateSelected)
            it.visibility = View.GONE
            voicePanel = it
        }
        setVoiceUi(if (textController.isPrivateField) VoiceUiState.Hidden else voiceController.initialState().toUiState())
        val content = LinearLayout(this).apply {
            id = R.id.ime_input_root
            orientation = LinearLayout.VERTICAL
            addView(strip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, candidateHeight))
            addView(keyboard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        keyboard.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateEmojiPickerLayout(candidateHeight)
            updateVoicePanelLayout(candidateHeight)
        }
        return FrameLayout(this).apply {
            addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(publicPicker, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, initialPickerHeight).apply {
                gravity = android.view.Gravity.TOP
            })
            addView(privatePicker, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, initialPickerHeight).apply {
                gravity = android.view.Gravity.TOP
            })
            addView(pickerBottomMask, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
            ).apply {
                gravity = android.view.Gravity.TOP
                topMargin = candidateHeight + initialPickerViewportHeight
            })
            addView(panel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, initialPickerHeight).apply {
                gravity = android.view.Gravity.TOP
            })
            updateEmojiPickerVisibility()
        }
    }

    internal open fun initialKeyboardBottomInset(): Int {
        val decorInsets = window.window?.decorView?.rootWindowInsets
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val decorBottom = decorInsets?.systemWindowInsetBottom ?: 0
            return resolveInitialKeyboardBottomInset(
                metricsBottom = null,
                decorBottom = decorBottom,
                legacyBottom = legacyNavigationBottomInset(),
            )
        }
        val decorBottom = decorInsets
            ?.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
            ?.bottom
            ?: 0
        val metricsBottom = getSystemService(WindowManager::class.java)
            ?.currentWindowMetrics
            ?.windowInsets
            ?.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
            ?.bottom
        // Current window metrics is authoritative whenever it is available, including a valid
        // zero for hardware navigation. Decor insets can belong to the pre-rotation or pre-fold
        // configuration while the IME window is being reconstructed.
        return resolveInitialKeyboardBottomInset(
            metricsBottom = metricsBottom,
            decorBottom = decorBottom,
            legacyBottom = legacyNavigationBottomInset(),
        )
    }

    private fun legacyNavigationBottomInset(): Int {
        val navigationShown = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        if (navigationShown != 0 && !resources.getBoolean(navigationShown)) return 0
        val navigationHeight = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (navigationHeight == 0) 0 else resources.getDimensionPixelSize(navigationHeight)
    }

    private fun updateVoicePanelLayout(candidateHeight: Int) {
        val panel = voicePanel ?: return
        val keyboard = keyboardView ?: return
        panel.layoutParams = (panel.layoutParams as? FrameLayout.LayoutParams ?: return).apply {
            height = candidateHeight + keyboard.emojiPickerOverlayHeight().toInt()
        }
        panel.requestLayout()
    }

    /** AndroidX owns its category header and grid; KeyboardView keeps only the fixed controls. */
    private fun updateEmojiPickerLayout(candidateHeight: Int) {
        val keyboard = keyboardView ?: return
        val viewportHeight = keyboard.emojiPickerOverlayHeight().toInt()
        val pickerHeight = candidateHeight + viewportHeight
        emojiPickerHeaderHeight = candidateHeight
        emojiPickerControlTop = candidateHeight + viewportHeight
        listOfNotNull(publicEmojiPicker, privateEmojiPicker).forEach { picker ->
            pickerViewportHeights[picker] = viewportHeight
            pickerViewportMaximums[picker] = viewportHeight
            // Keyboard width/height or a preset can change the AndroidX cell geometry.
            pickerViewportLocked.remove(picker)
            pickerViewportCategoryTransitions.remove(picker)
            picker.layoutParams = (picker.layoutParams as? FrameLayout.LayoutParams ?: return@forEach).apply {
                height = pickerHeight
            }
            picker.requestLayout()
            bindEmojiPickerViewport(picker)
        }
        updateEmojiPickerMask()
    }

    private fun updateEmojiPickerVisibility() {
        val isEmoji = keyboardMode == KeyboardMode.EMOJI
        val isVoice = keyboardMode == KeyboardMode.VOICE
        val isPrivate = ::textController.isInitialized && textController.isPrivateField
        candidateStrip?.visibility = if (isEmoji || isVoice || isPrivate) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
        val showPrivate = isEmoji && ::textController.isInitialized && textController.isPrivateField
        publicEmojiPicker?.visibility = if (isEmoji && !showPrivate) View.VISIBLE else View.GONE
        privateEmojiPicker?.visibility = if (showPrivate) View.VISIBLE else View.GONE
        emojiPickerBottomMask?.visibility = if (isEmoji) View.VISIBLE else View.GONE
        voicePanel?.visibility = if (isVoice && !isPrivate) View.VISIBLE else View.GONE
        updateEmojiPickerMask()
    }

    private fun createEmojiPicker(provider: RecentEmojiProvider): EmojiPickerView = EmojiPickerView(this).apply {
        setBackgroundColor(getColor(R.color.keyboard_background))
        emojiGridColumns = 8
        emojiGridRows = 3f
        // AndroidX invokes this listener before it records its selection and marks Recent
        // dirty. A successful public commit therefore installs a fresh provider afterward.
        setOnEmojiPickedListener(Consumer { item -> onKeyAction(KeyAction.CommitEmoji(item.emoji)) })
        setRecentEmojiProvider(provider)
        setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                post { bindEmojiPickerViewport(this@apply) }
            }

            override fun onChildViewRemoved(parent: View, child: View) = Unit
        })
        viewTreeObserver.addOnGlobalLayoutListener { bindEmojiPickerViewport(this@apply) }
        addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
            val picker = view as EmojiPickerView
            val signature = (right - left) to (bottom - top)
            if (signature.first <= 0 || signature.second <= 0 || pickerLayoutSignatures[picker] == signature) return@addOnLayoutChangeListener
            pickerLayoutSignatures[picker] = signature
            // BundledEmojiListLoader creates the header/body asynchronously.  The initial
            // loader build already uses the configured rows and columns, so it must not be
            // rebuilt while its views are absent.  Only a later external size change needs
            // an adapter refresh.
            if (!picker.hasInflatedEmojiPickerContent()) return@addOnLayoutChangeListener
            bindEmojiPickerViewport(picker)
            picker.post {
                if (pickerLayoutSignatures[picker] == signature && picker.hasInflatedEmojiPickerContent()) {
                    picker.emojiGridColumns = 8
                    picker.emojiGridRows = 3f
                }
            }
        }
    }

    private fun bindEmojiPickerViewport(picker: EmojiPickerView) {
        val header = picker.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_header) ?: return
        if (pickerHeaders[picker] !== header) {
            // AndroidX's default manager divides phone width among all ten categories, which
            // produces 40dp targets. Keep all categories but let the 48dp holders scroll.
            header.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
            installEmojiCategoryTapListeners(picker, header)
            pickerHeaders[picker] = header
        }
        val categoryWidth = resources.getDimensionPixelSize(R.dimen.emoji_picker_header_icon_holder_width)
        repeat(header.childCount) { enforceEmojiCategoryHolderWidth(header.getChildAt(it), categoryWidth) }
        val body = picker.findViewById<RecyclerView>(androidx.emoji2.emojipicker.R.id.emoji_picker_body) ?: return
        ensureEmojiPickerBodyOverscan(picker, body)
        if (pickerBodies[picker] !== body) {
            // An AndroidX grid rebuild replaces this RecyclerView. Any callback captured by
            // the old body must not settle geometry for its replacement.
            pickerViewportCategoryTransitions.remove(picker)
            pickerViewportLocked.remove(picker)
            pickerBodies.put(picker, body)?.let {
                pickerBodyOverscanPrepared.remove(it)
                pickerBodyOverscanPending.remove(it)
            }
            body.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> onEmojiPickerBodyChanged(picker, body) }
            body.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    body.post {
                        finishEmojiPickerBodyOverscan(picker, body)
                        onEmojiPickerBodyChanged(picker, body)
                    }
                }
                override fun onChildViewDetachedFromWindow(view: View) = Unit
            })
            body.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dx != 0 || dy != 0) body.post { onEmojiPickerBodyChanged(picker, body) }
                }
            })
        }
        if (body.childCount > 0) {
            // A size change can rebind an already populated body. Its initial cell creation
            // is complete, so restore the wrapper-owned final height synchronously.
            finishEmojiPickerBodyOverscan(picker, body)
            body.post { onEmojiPickerBodyChanged(picker, body) }
        }
    }

    /**
     * AndroidX reads this provisional child height while creating its adapter cells. The
     * enclosing wrapper remains the final viewport height and clips the child at controls.
     */
    private fun ensureEmojiPickerBodyOverscan(picker: EmojiPickerView, body: RecyclerView) {
        val maximumViewport = pickerViewportMaximums[picker] ?: return
        val params = body.layoutParams ?: return
        if (!pickerBodyOverscanPrepared.add(body)) return
        pickerBodyOverscanPending += body
        val provisionalHeight = maximumViewport + pxForDp(EMOJI_PICKER_BODY_SPACER_DP)
        if (params.height != provisionalHeight) {
            params.height = provisionalHeight
            body.layoutParams = params
            body.requestLayout()
        }
    }

    /** Restore the wrapper-owned final body height once AndroidX has attached its first cell. */
    private fun finishEmojiPickerBodyOverscan(picker: EmojiPickerView, body: RecyclerView) {
        if (pickerBodies[picker] !== body || !pickerBodyOverscanPending.remove(body)) return
        val params = body.layoutParams ?: return
        if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            body.layoutParams = params
            body.requestLayout()
        }
    }

    private fun installEmojiCategoryTapListeners(picker: EmojiPickerView, header: RecyclerView) {
        val categoryWidth = resources.getDimensionPixelSize(R.dimen.emoji_picker_header_icon_holder_width)
        fun install(holder: View) {
            enforceEmojiCategoryHolderWidth(holder, categoryWidth)
            var downX = 0f
            var downY = 0f
            val touchSlop = ViewConfiguration.get(holder.context).scaledTouchSlop
            holder.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        if (kotlin.math.abs(event.x - downX) <= touchSlop &&
                            kotlin.math.abs(event.y - downY) <= touchSlop
                        ) {
                            beginEmojiCategoryTransition(picker, header.getChildAdapterPosition(holder))
                        }
                    }
                }
                // AndroidX owns the holder click listener that changes the selected group.
                false
            }
            holder.isFocusable = true
            holder.setOnKeyListener { _, keyCode, event ->
                if (isEmojiCategoryActivationKey(keyCode, event.action)) {
                    beginEmojiCategoryTransition(picker, header.getChildAdapterPosition(holder))
                }
                // AndroidX's click action remains responsible for category selection.
                false
            }
            ViewCompat.setAccessibilityDelegate(holder, object : androidx.core.view.AccessibilityDelegateCompat() {
                override fun performAccessibilityAction(host: View, action: Int, arguments: android.os.Bundle?): Boolean {
                    if (isEmojiCategoryAccessibilityAction(action)) {
                        beginEmojiCategoryTransition(picker, header.getChildAdapterPosition(holder))
                    }
                    return super.performAccessibilityAction(host, action, arguments)
                }
            })
        }
        header.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) = install(view)
            override fun onChildViewDetachedFromWindow(view: View) = Unit
        })
        repeat(header.childCount) { install(header.getChildAt(it)) }
    }

    private fun beginEmojiCategoryTransition(picker: EmojiPickerView, targetCategory: Int) {
        val body = pickerBodies[picker] ?: return
        val generation = ++emojiCategoryTransitionGeneration
        val currentBaseline = EmojiViewportBaseline(
            viewportHeight = pickerViewportHeights[picker],
            wasLocked = picker in pickerViewportLocked,
            clipBounds = body.clipBounds?.let { Rect(it) },
        )
        val baseline = nextEmojiCategoryBaseline(
            pendingBaseline = pickerViewportCategoryTransitions[picker]?.baseline,
            currentBaseline = currentBaseline,
        )
        pickerViewportLocked.remove(picker)
        pickerViewportCategoryTransitions[picker] = EmojiCategoryTransition(
            generation = generation,
            targetCategory = targetCategory,
            baseline = baseline,
        )
        // The AndroidX click scrolls after this non-consuming callback. New body content,
        // rather than a frame boundary, decides when its three rows can be measured.
        // This also handles re-tapping the already selected category without waiting for a
        // RecyclerView scroll callback.
        body.post { onEmojiPickerBodyChanged(picker, body) }
    }

    private fun onEmojiPickerBodyChanged(picker: EmojiPickerView, body: RecyclerView) {
        if (!shouldApplyEmojiPickerViewport(pickerBodies[picker], body)) return
        pickerViewportHeights[picker]?.let { updateEmojiPickerCellAccessibility(body, it) }
        val transition = pickerViewportCategoryTransitions[picker]
        if (transition == null) {
            applyEmojiPickerViewport(picker, body)
            return
        }
        // Old content remains attached briefly while AndroidX scrolls to the selected
        // category. Its three rows are valid geometry for the old category only.
        if (!isTargetEmojiCategoryAtBodyStart(body, transition.targetCategory)) return
        val placeholderVisible = hasVisibleEmojiEmptyCategoryPlaceholder(body)
        val observedViewport = if (placeholderVisible) {
            emptyRecentViewport(body)
        } else {
            emojiThreeRowViewport(body)
        }
        if (isEmojiCategoryContentReady(transition.targetCategory, observedViewport, placeholderVisible)) {
            pickerViewportCategoryTransitions.remove(picker)
            applyEmojiPickerViewport(picker, body)
        }
    }

    private fun applyEmojiPickerViewport(picker: EmojiPickerView, body: RecyclerView) {
        if (!shouldApplyEmojiPickerViewport(pickerBodies[picker], body)) {
            return
        }
        val presetViewport = pickerViewportMaximums[picker]
        val emptyRecentVisible = hasVisibleEmojiEmptyCategoryPlaceholder(body)
        val observedViewport = if (emptyRecentVisible) {
            boundedEmojiViewport(presetViewport, emptyRecentViewport(body))
        } else {
            boundedEmojiViewport(presetViewport, emojiThreeRowViewport(body))
        }
        val lockedViewport = pickerViewportHeights[picker].takeIf { picker in pickerViewportLocked }
        val viewportHeight = resolveEmojiViewportWithPlaceholder(
            lockedViewport, presetViewport, observedViewport, emptyRecentVisible,
        )
            ?: pickerViewportHeights[picker]
            ?: return
        if (pickerViewportCategoryTransitions.containsKey(picker)) {
            body.clipBounds = Rect(0, 0, body.width, viewportHeight)
            updateEmojiPickerCellAccessibility(body, viewportHeight)
            return
        }
        // The empty Recent placeholder is followed by the next category. Wait until both
        // complete rows after that placeholder are attached before establishing the clip.
        if (emptyRecentVisible && observedViewport == null && lockedViewport == null) {
            val temporaryViewport = presetViewport ?: viewportHeight
            body.clipBounds = Rect(0, 0, body.width, temporaryViewport)
            updateEmojiPickerCellAccessibility(body, temporaryViewport)
            updateEmojiPickerMask()
            return
        }
        if (emptyRecentVisible) {
            pickerViewportHeights[picker] = viewportHeight
            pickerViewportLocked += picker
            body.clipBounds = Rect(0, 0, body.width, viewportHeight)
            updateEmojiPickerCellAccessibility(body, viewportHeight)
            updateEmojiPickerMask()
            return
        }
        // Let AndroidX create its cells from the one-spacer-taller parent first. The actual
        // EmojiView bounds, rather than a preset estimate, then define the three-row body.
        if (observedViewport == null) {
            // Robolectric and the first loader frame can have an adapter before it attaches
            // EmojiViews. Keep its measurement for cell creation but clip its visible area.
            body.clipBounds = Rect(0, 0, body.width, viewportHeight)
            updateEmojiPickerCellAccessibility(body, viewportHeight)
            return
        }
        if (lockedViewport != null) {
            updateEmojiPickerCellAccessibility(body, viewportHeight)
            return
        }
        pickerViewportHeights[picker] = viewportHeight
        pickerViewportLocked += picker
        body.clipBounds = Rect(0, 0, body.width, viewportHeight)
        updateEmojiPickerCellAccessibility(body, viewportHeight)
        updateEmojiPickerMask()
    }

    private fun updateEmojiPickerCellAccessibility(body: RecyclerView, visibleViewportHeight: Int) {
        val viewport = Rect(0, 0, body.width, visibleViewportHeight)
        fun visit(view: View) {
            if (view.javaClass.name == "androidx.emoji2.emojipicker.EmojiView") {
                val bounds = Rect(0, 0, view.width, view.height)
                body.offsetDescendantRectToMyCoords(view, bounds)
                view.importantForAccessibility = if (isEmojiCellFullyVisibleInViewport(bounds, viewport)) {
                    View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                } else {
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                }
            }
            if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) }
        }
        visit(body)
    }

    private fun updateEmojiPickerMask() {
        val activePicker = when {
            privateEmojiPicker?.visibility == View.VISIBLE -> privateEmojiPicker
            publicEmojiPicker?.visibility == View.VISIBLE -> publicEmojiPicker
            else -> null
        } ?: return
        val visibleBodyHeight = pickerViewportHeights[activePicker] ?: return
        emojiPickerBottomMask?.let { mask ->
            val params = mask.layoutParams as? FrameLayout.LayoutParams ?: return@let
            val topMargin = emojiPickerHeaderHeight + visibleBodyHeight
            val height = (emojiPickerControlTop - topMargin).coerceAtLeast(0)
            if (shouldUpdateEmojiPickerMask(params.topMargin, params.height, topMargin, height)) {
                params.topMargin = topMargin
                params.height = height
                mask.layoutParams = params
            }
        }
    }

    private fun EmojiPickerView.hasInflatedEmojiPickerContent(): Boolean =
        findViewById<View>(androidx.emoji2.emojipicker.R.id.emoji_picker_header) != null &&
            findViewById<View>(androidx.emoji2.emojipicker.R.id.emoji_picker_body) != null

    private fun pxForDp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun publicRecentProvider(): RecentEmojiProvider = object : RecentEmojiProvider {
            override fun recordSelection(emoji: String) = Unit
            override suspend fun getRecentEmojiList(): List<String> = ImePreferences.getEmojiRecents(this@ImeService)
        }

    private fun privateRecentProvider(): RecentEmojiProvider = object : RecentEmojiProvider {
        override fun recordSelection(emoji: String) = Unit
        override suspend fun getRecentEmojiList(): List<String> = emptyList()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        if (keyboardMode == KeyboardMode.VOICE) cancelVoiceSession() else cancelVoiceHold()
        finishEnglishRaw()
        finishSlashRaw()
        setVoiceUi(VoiceUiState.Hidden)
        keyboardView?.cancelActiveGestures()
        super.onFinishInputView(finishingInput)
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (keyboardMode == KeyboardMode.VOICE) cancelVoiceSession() else cancelVoiceHold()
        keyboardView?.setHeightPreset(ImePreferences.getKeyboardHeightPreset(this))
        keyboardView?.setEmojiRecents(ImePreferences.getEmojiRecents(this))
        keyboardView?.refreshIntrinsicLayout()
        setVoiceUi(if (textController.isPrivateField) VoiceUiState.Hidden else voiceController.initialState().toUiState())
        updateEmojiPickerVisibility()
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        editorSession.advance()
        if (keyboardMode == KeyboardMode.VOICE) cancelVoiceSession() else cancelVoiceHold()
        invalidateConversion(clearComposing = false)
        invalidateEnglish(clearComposing = false)
        invalidateSlash(clearComposing = false)
        textController.beginInput(attribute)
        textController.terminalCursorEnabled = ImePreferences.isTerminalCursorEnabled(this)
        updateEmojiPickerVisibility()
        setVoiceUi(if (textController.isPrivateField) VoiceUiState.Hidden else voiceController.initialState().toUiState())
        keyboardMode = ImePreferences.getLastKeyboardMode(this)
        keyboardView?.setMode(keyboardMode)
        keyboardView?.setDualFlickEnabled(ImePreferences.isDualFlickEnabled(this))
        keyboardView?.setHeightPreset(ImePreferences.getKeyboardHeightPreset(this))
        keyboardView?.setEmojiRecents(ImePreferences.getEmojiRecents(this))
        updateEmojiPickerVisibility()
    }

    override fun onFinishInput() {
        if (keyboardMode == KeyboardMode.VOICE) cancelVoiceSession() else cancelVoiceHold()
        finishEnglishRaw()
        finishSlashRaw()
        editorSession.advance()
        invalidateConversion(clearComposing = false)
        keyboardView?.cancelActiveGestures()
        textController.finishComposition()
        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (candidateSource == CandidateSource.PREDICTION || predictionRequestInFlight) {
            if (!isExpectedPredictionSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd)) {
                invalidateConversion(clearComposing = false)
            }
        } else {
            pendingPredictionSelection = null
        }
        if (reading.isNotEmpty() && (candidatesStart < 0 || newSelStart !in candidatesStart..candidatesEnd)) {
            invalidateConversion(clearComposing = false)
            textController.abandonComposition()
        }
        if (englishBuffer.isNotEmpty() && (
                candidatesStart < 0 || newSelStart != newSelEnd || newSelEnd != candidatesEnd
            )
        ) {
            textController.finishComposition()
            invalidateEnglish(clearComposing = false)
        }
        if (slashBufferActive && (candidatesStart < 0 || newSelStart != newSelEnd || newSelEnd != candidatesEnd)) {
            textController.finishComposition()
            invalidateSlash(clearComposing = false)
        }
    }

    override fun onKeyAction(action: KeyAction) {
        val voiceCandidateSelection = action is KeyAction.SelectCandidate && candidateSource == CandidateSource.VOICE
        val voiceEditingSession = continuousVoiceEditingSessionFor(action)
        // A destructive voice-layer action must invalidate a queued candidate tap before the
        // action mutex reaches it. This closes the small queueing window after a user cancels
        // or flicks away from the layer and before a preview tap could commit/restart.
        if (keyboardMode == KeyboardMode.VOICE &&
            (action == KeyAction.CancelVoice || action is KeyAction.SwitchLayer)
        ) {
            invalidateContinuousVoiceSession()
        }
        if (!voiceCandidateSelection && voiceEditingSession == null) {
            cancelVoiceHold()
            if (!textController.isPrivateField) setVoiceUi(voiceController.initialState().toUiState())
        }
        val queuedForEditor = editorSession.capture()
        val queuedCandidateSnapshot = if (action is KeyAction.SelectCandidate) candidateSnapshot() else null
        serviceScope.launch {
            actionMutex.withLock {
                if (!editorSession.isCurrent(queuedForEditor)) return@withLock
                if (queuedCandidateSnapshot != null && queuedCandidateSnapshot != candidateSnapshot()) return@withLock
                runCatching { processInputAction(action, queuedForEditor, voiceEditingSession) }
                    .onFailure {
                        clearCandidateState()
                        candidateStrip?.showStatus("変換を利用できません")
                    }
            }
        }
    }

    private fun onCandidateSelected(event: CandidateUiEvent) {
        if (event.token != candidateUiToken) return
        onKeyAction(KeyAction.SelectCandidate(event.index))
    }

    private fun onCandidateLongPressed(event: CandidateUiLongPressEvent): Boolean {
        if (!isEligibleHistoryLongPress(event, candidateUiToken, candidateSource in HISTORY_CANDIDATE_SOURCES, conversionCandidates)) return false
        val source = candidateSource
        val editorToken = editorSession.capture()
        serviceScope.launch {
            actionMutex.withLock {
                if (!editorSession.isCurrent(editorToken) ||
                    candidateSource != source ||
                    !isEligibleHistoryLongPress(event, candidateUiToken, candidateSource in HISTORY_CANDIDATE_SOURCES, conversionCandidates)
                ) return@withLock
                val state = conversionEngine.deleteCandidateFromHistory(event.index) ?: return@withLock
                if (event.token != candidateUiToken || !editorSession.isCurrent(editorToken)) return@withLock
                if (source == CandidateSource.PREDICTION) applyPrediction(state) else applyConversion(state)
            }
        }
        return true
    }

    override fun onVoiceHold(event: VoiceHoldEvent) {
        when (event) {
            is VoiceHoldEvent.Begin -> {
                cancelVoiceHold()
                if (textController.isPrivateField) return
                val token = editorSession.capture()
                voiceHoldRequestId = event.requestId
                voiceHoldEditorToken = token
                serviceScope.launch {
                    actionMutex.withLock {
                        if (voiceHoldRequestId != event.requestId || !editorSession.isCurrent(token)) return@withLock
                        finishEnglishRaw()
                        finishSlashRaw()
                        resetConversion(clearComposing = false)
                        if (voiceHoldRequestId == event.requestId && editorSession.isCurrent(token)) voiceController.start(token)
                    }
                }
            }
            is VoiceHoldEvent.End -> {
                if (voiceHoldRequestId != event.requestId) return
                voiceHoldReleased = true
                val text = voiceController.confirm(voiceHoldEditorToken)
                if (text != null) commitVoiceHold(event.requestId, voiceHoldEditorToken, text)
                else if (voiceHoldReady) voiceController.stop() else cancelVoiceHoldAndResetUi()
            }
            is VoiceHoldEvent.Cancel -> if (voiceHoldRequestId == event.requestId) cancelVoiceHoldAndResetUi()
        }
    }

    private fun cancelVoiceHold() {
        voiceHoldGeneration++
        voiceHoldRequestId = null
        voiceHoldReady = false
        voiceHoldReleased = false
        voiceController.cancel(notify = false)
    }

    private fun cancelVoiceHoldAndResetUi() {
        cancelVoiceHold()
        if (::textController.isInitialized && !textController.isPrivateField) {
            setVoiceUi(voiceController.initialState().toUiState())
        }
    }

    private fun continuousVoiceEditingSessionFor(action: KeyAction): Long? {
        if (keyboardMode != KeyboardMode.VOICE) return null
        return when (action) {
            is KeyAction.CommitText,
            is KeyAction.Backspace,
            KeyAction.Enter,
            KeyAction.Paste,
            is KeyAction.MoveCursor,
            is KeyAction.ModifiedKey,
            -> voiceSessionGeneration
            else -> null
        }
    }

    private suspend fun processInputAction(action: KeyAction, editorToken: Long, voiceEditingSession: Long? = null) {
        if (voiceEditingSession != null) {
            if (!isCurrentContinuousVoiceSession(voiceEditingSession, editorToken)) return
            processContinuousVoiceEditingAction(action, editorToken)
            return
        }
        if (slashBufferActive && action !is KeyAction.SelectCandidate && action !is KeyAction.Backspace) {
            finishSlashRaw()
        }
        when (action) {
            is KeyAction.CommitText -> {
                if (shouldStartSlashCandidates(action.text)) startSlashCandidates()
                else if (shouldBufferEnglish(action.text)) appendEnglish(action.text, editorToken)
                else if (action.text == " " && reading.isNotEmpty()) cycleCandidate()
                else {
                    finishEnglishRaw()
                    resetConversion(clearComposing = false)
                    editorSession.runIfCurrent(editorToken) { textController.commitText(action.text) }
                }
            }
            is KeyAction.CommitEmoji -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                if (editorSession.isCurrent(editorToken) && textController.commitText(action.text) && !textController.isPrivateField) {
                    keyboardView?.setEmojiRecents(ImePreferences.recordEmojiRecent(this, action.text))
                    // Refresh the visible Recent group only after the guarded editor commit
                    // and preference write; AndroidX's listener runs before both.
                    publicEmojiPicker?.setRecentEmojiProvider(publicRecentProvider())
                }
            }
            is KeyAction.KanaInput -> {
                finishEnglishRaw()
                clearPredictionIfShown()
                restoreReadingPreview()
                updateReading(textController.appendComposing(action.reading))
            }
            is KeyAction.TransformKana -> {
                finishEnglishRaw()
                clearPredictionIfShown()
                restoreReadingPreview()
                updateReading(textController.transformKana(action.transform))
            }
            is KeyAction.Backspace -> {
                if (slashBufferActive) {
                    textController.backspace()
                    invalidateSlash(clearComposing = false)
                    return
                }
                if (englishBuffer.isNotEmpty()) {
                    val remaining = textController.backspace()
                    englishBuffer = remaining
                    if (remaining.isEmpty()) invalidateEnglish(clearComposing = false)
                    else requestEnglishSuggestions(remaining, editorToken)
                    return
                }
                restoreReadingPreview()
                val remaining = textController.backspace()
                if (reading.isNotEmpty()) updateReading(remaining)
            }
            KeyAction.Enter -> {
                if (englishBuffer.isNotEmpty()) {
                    finishEnglishRaw()
                } else if (reading.isNotEmpty()) {
                    if (candidates.isEmpty()) {
                        val generation = ++conversionGeneration
                        val state = conversionEngine.nextCandidate()
                        if (generation != conversionGeneration || !editorSession.isCurrent(editorToken)) return
                        applyConversion(state)
                    }
                    if (candidates.isNotEmpty()) commitJapaneseCandidate(selectedCandidate.coerceAtLeast(0), editorToken)
                    else {
                        expectPredictionSelectionAfterCommit(reading, reading)
                        if (!editorSession.runIfCurrent(editorToken) { textController.commitCandidate(reading) }) {
                            pendingPredictionSelection = null
                            return
                        }
                        resetConversion(clearComposing = false)
                        requestNextWordPrediction(editorToken)
                    }
                } else textController.enter()
            }
            KeyAction.Paste -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.paste() }
            }
            KeyAction.Escape -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.escape() }
            }
            KeyAction.CommitConversion -> {
                finishEnglishRaw()
                commitDisplayedConversion(editorToken)
            }
            KeyAction.CommitWithoutConversion -> {
                finishEnglishRaw()
                commitConversionAs(reading, editorToken)
            }
            KeyAction.ConvertToKatakana -> {
                finishEnglishRaw()
                commitConversionAs(reading.toKatakana(), editorToken)
            }
            is KeyAction.MoveCursor -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.moveCursor(action.direction, action.units) }
            }
            is KeyAction.MoveToBoundary -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) { textController.moveToBoundary(action.boundary) }
            }
            is KeyAction.ModifiedKey -> {
                finishEnglishRaw()
                resetConversion(clearComposing = false)
                editorSession.runIfCurrent(editorToken) {
                    textController.sendModifiedKey(action.label, action.modifier)
                }
            }
            is KeyAction.SwitchLayer -> {
                finishEnglishRaw()
                if (keyboardMode == KeyboardMode.VOICE) cancelVoiceSession()
                ImePreferences.setLastKeyboardMode(this, action.target)
                keyboardMode = action.target
                keyboardView?.setMode(action.target)
                updateEmojiPickerVisibility()
            }
            is KeyAction.SelectCandidate -> {
                if (candidateSource == CandidateSource.VOICE) commitVoiceCandidate(action.index, editorToken)
                else if (candidateSource == CandidateSource.ENGLISH) commitEnglishCandidate(action.index)
                else if (candidateSource == CandidateSource.SLASH) commitSlashCandidate(action.index)
                else if (candidateSource == CandidateSource.PREDICTION) commitPredictionCandidate(action.index, editorToken)
                else commitJapaneseCandidate(action.index, editorToken)
            }
            KeyAction.CycleCandidate -> cycleCandidate()
            is KeyAction.SetModifier -> finishEnglishRaw()
            KeyAction.VoiceHold -> startVoiceLayer(editorToken)
            KeyAction.CancelVoice -> cancelVoiceSession()
        }
    }

    /**
     * The bottom row of VOICE intentionally edits the target without altering its recognizer,
     * voice panel, candidates, or Mozc composition state.  Its session token also drops a tap
     * that was queued before cancel/layer change invalidated the continuous session.
     */
    private fun processContinuousVoiceEditingAction(action: KeyAction, editorToken: Long) {
        editorSession.runIfCurrent(editorToken) {
            when (action) {
                is KeyAction.CommitText -> textController.commitText(action.text)
                is KeyAction.Backspace -> textController.backspace()
                KeyAction.Enter -> textController.enter()
                KeyAction.Paste -> textController.paste()
                is KeyAction.MoveCursor -> textController.moveCursor(action.direction, action.units)
                is KeyAction.ModifiedKey -> textController.sendModifiedKey(action.label, action.modifier)
                else -> Unit
            }
        }
    }

    private suspend fun startVoiceLayer(editorToken: Long) {
        if (textController.isPrivateField || keyboardMode == KeyboardMode.VOICE) return
        finishEnglishRaw()
        finishSlashRaw()
        resetConversion(clearComposing = false)
        voiceReturnMode = keyboardMode
        keyboardMode = KeyboardMode.VOICE
        keyboardView?.setMode(KeyboardMode.VOICE)
        keyboardView?.setVoiceSessionActive(false)
        updateEmojiPickerVisibility()
        candidateSource = CandidateSource.VOICE
        candidates = emptyList()
        showCandidateStrip(emptyList(), -1)
        voiceSessionGeneration++
        startContinuousVoiceRecognition(editorToken, voiceSessionGeneration)
    }

    private fun commitVoiceCandidate(index: Int, token: Long) {
        val session = voiceSessionGeneration
        if (!isCurrentContinuousVoiceSession(session, token)) return
        val text = voiceController.confirm(token, index) ?: return
        if (!isCurrentContinuousVoiceSession(session, token)) return
        var committed = false
        if (!editorSession.runIfCurrent(token) { committed = textController.commitText(text) } || !committed) {
            // confirm() has already destroyed the recognizer. Do not leave a blank VOICE layer
            // with no session when the editor rejects the one allowed commit.
            if (isCurrentContinuousVoiceSession(session, token)) cancelVoiceSession()
            return
        }
        if (!isCurrentContinuousVoiceSession(session, token)) return
        // confirm() synchronously clears the preview through onVoiceState(Idle). Restart only
        // after that clear and the guarded single editor commit have both completed.
        startContinuousVoiceRecognition(token, session)
    }

    private fun cancelVoiceSession() {
        invalidateContinuousVoiceSession()
        leaveVoiceLayer()
    }

    private fun invalidateContinuousVoiceSession() {
        voiceSessionGeneration++
        voiceController.cancel(notify = false)
        keyboardView?.setVoiceSessionActive(false)
    }

    private fun startContinuousVoiceRecognition(token: Long, session: Long) {
        if (!isCurrentContinuousVoiceSession(session, token)) return
        voiceController.start(token)
    }

    private fun isCurrentContinuousVoiceSession(session: Long, token: Long): Boolean =
        session == voiceSessionGeneration &&
            keyboardMode == KeyboardMode.VOICE &&
            editorSession.isCurrent(token) &&
            !textController.isPrivateField

    private fun leaveVoiceLayer() {
        keyboardView?.setVoiceSessionActive(false)
        candidateSource = CandidateSource.NONE
        candidates = emptyList()
        showCandidateStrip(emptyList(), -1)
        if (keyboardMode == KeyboardMode.VOICE) {
            keyboardMode = voiceReturnMode
            keyboardView?.setMode(voiceReturnMode)
            updateEmojiPickerVisibility()
        }
        if (!textController.isPrivateField) setVoiceUi(voiceController.initialState().toUiState())
    }

    private fun shouldBufferEnglish(text: String): Boolean =
        ImePreferences.isEnglishSuggestionsEnabled(this) &&
            !textController.isPrivateField &&
            text.length == 1 && text[0].isAsciiLetterOrDigit()

    private fun shouldStartSlashCandidates(text: String): Boolean =
        text == "/" && !textController.isPrivateField

    private suspend fun startSlashCandidates() {
        finishEnglishRaw()
        if (reading.isNotEmpty()) textController.finishComposition()
        resetConversion(clearComposing = false)
        textController.appendComposing("/")
        slashBufferActive = true
        slashGeneration++
        candidateSource = CandidateSource.SLASH
        candidates = ImePreferences.getSlashCommandCandidates(this)
        selectedCandidate = -1
        showCandidateState()
    }

    private fun commitSlashCandidate(index: Int) {
        if (!slashBufferActive || candidateSource != CandidateSource.SLASH || index !in candidates.indices) return
        textController.commitCandidate(candidates[index])
        invalidateSlash(clearComposing = false)
    }

    private fun finishSlashRaw() {
        if (!slashBufferActive) return
        textController.finishComposition()
        invalidateSlash(clearComposing = false)
    }

    private fun invalidateSlash(clearComposing: Boolean) {
        slashGeneration++
        slashBufferActive = false
        if (clearComposing) textController.cancelComposition()
        if (candidateSource == CandidateSource.SLASH) {
            candidateSource = CandidateSource.NONE
            candidates = emptyList()
            selectedCandidate = -1
            showCandidateState()
        }
    }

    private fun appendEnglish(text: String, editorToken: Long) {
        if (reading.isNotEmpty()) {
            textController.finishComposition()
            clearCandidateState()
        }
        if (englishBuffer.length >= MAX_ENGLISH_BUFFER) finishEnglishRaw()
        englishBuffer = textController.appendComposing(text)
        candidateSource = CandidateSource.ENGLISH
        requestEnglishSuggestions(englishBuffer, editorToken)
    }

    private fun requestEnglishSuggestions(prefix: String, editorToken: Long) {
        val generation = ++englishGeneration
        candidateSource = CandidateSource.ENGLISH
        candidates = emptyList()
        selectedCandidate = -1
        showCandidateState()
        serviceScope.launch {
            val suggestions = runCatching { englishSuggestionEngine.suggest(prefix, MAX_ENGLISH_CANDIDATES) }
                .getOrDefault(emptyList())
            actionMutex.withLock {
                if (!editorSession.isCurrent(editorToken) || textController.isPrivateField ||
                    generation != englishGeneration || englishBuffer != prefix ||
                    candidateSource != CandidateSource.ENGLISH
                ) return@withLock
                candidates = suggestions.take(MAX_ENGLISH_CANDIDATES)
                selectedCandidate = -1
                showCandidateState()
            }
        }
    }

    private fun commitEnglishCandidate(index: Int) {
        if (index !in candidates.indices || candidateSource != CandidateSource.ENGLISH) return
        textController.commitCandidate(candidates[index])
        invalidateEnglish(clearComposing = false)
    }

    private fun finishEnglishRaw() {
        if (englishBuffer.isEmpty()) return
        textController.finishComposition()
        invalidateEnglish(clearComposing = false)
    }

    private fun invalidateEnglish(clearComposing: Boolean) {
        englishGeneration++
        englishBuffer = ""
        if (clearComposing) textController.cancelComposition()
        if (candidateSource == CandidateSource.ENGLISH) {
            candidateSource = CandidateSource.NONE
            candidates = emptyList()
            selectedCandidate = -1
            showCandidateState()
        }
    }

    private fun showCandidateState() {
        showCandidateStrip(candidates, selectedCandidate)
        keyboardView?.setCandidates(candidates, selectedCandidate)
    }

    private fun showCandidateStrip(
        values: List<String>,
        selectedIndex: Int,
        selectable: Boolean = true,
    ) {
        candidateStrip?.showCandidates(CandidateUiSnapshot(++candidateUiToken, values, selectedIndex, selectable))
    }

    private fun showVoiceCandidates(
        values: List<String>,
        selectable: Boolean,
    ) {
        voicePanel?.showCandidates(CandidateUiSnapshot(++candidateUiToken, values, selectable = selectable))
    }

    private fun candidateSnapshot() = CandidateSnapshot(
        source = candidateSource,
        candidates = candidates,
        conversionGeneration = conversionGeneration,
        englishGeneration = englishGeneration,
        englishBuffer = englishBuffer,
        slashGeneration = slashGeneration,
        slashBufferActive = slashBufferActive,
        predictionGeneration = predictionGeneration,
    )

    private suspend fun updateReading(newReading: String) {
        conversionPreview = null
        if (textController.isPrivateField || newReading.isEmpty()) {
            resetConversion(clearComposing = false)
            return
        }
        val wasEmpty = reading.isEmpty()
        reading = newReading
        keyboardView?.setConversionActive(true)
        val generation = ++conversionGeneration
        val state = if (wasEmpty) conversionEngine.start(newReading) else conversionEngine.update(newReading)
        if (generation == conversionGeneration && reading == newReading) applyConversion(state)
    }

    private suspend fun cycleCandidate() {
        if (reading.isEmpty()) {
            textController.commitText(" ")
            return
        }
        conversionPreview = null
        textController.replaceComposing(reading)
        val generation = ++conversionGeneration
        val state = conversionEngine.nextCandidate()
        if (generation == conversionGeneration) applyConversion(state)
    }

    private suspend fun commitJapaneseCandidate(index: Int, editorToken: Long) {
        if (index !in candidates.indices) return
        val generation = ++conversionGeneration
        val result = conversionEngine.commit(index)
        if (generation != conversionGeneration || !editorSession.isCurrent(editorToken)) return
        val committed = result?.value ?: run {
            clearCandidateState()
            return
        }
        expectPredictionSelectionAfterCommit(committed, reading)
        textController.commitCandidate(committed)
        clearCandidateState()
        requestNextWordPrediction(editorToken)
    }

    private suspend fun commitDisplayedConversion(editorToken: Long) {
        val preview = conversionPreview
        if (preview != null) {
            expectPredictionSelectionAfterCommit(preview, preview)
            if (!editorSession.runIfCurrent(editorToken) { textController.commitCandidate(preview) }) {
                pendingPredictionSelection = null
                return
            }
            resetConversion(clearComposing = false)
            requestNextWordPrediction(editorToken)
        } else if (candidates.isNotEmpty()) {
            commitJapaneseCandidate(selectedCandidate.coerceAtLeast(0), editorToken)
        } else if (reading.isNotEmpty()) {
            expectPredictionSelectionAfterCommit(reading, reading)
            if (!editorSession.runIfCurrent(editorToken) { textController.commitCandidate(reading) }) {
                pendingPredictionSelection = null
                return
            }
            resetConversion(clearComposing = false)
            requestNextWordPrediction(editorToken)
        }
    }

    private suspend fun commitConversionAs(value: String, editorToken: Long) {
        if (reading.isEmpty()) return
        expectPredictionSelectionAfterCommit(value, reading)
        if (!editorSession.runIfCurrent(editorToken) { textController.commitCandidate(value) }) {
            pendingPredictionSelection = null
            return
        }
        resetConversion(clearComposing = false)
        requestNextWordPrediction(editorToken)
    }

    private suspend fun commitPredictionCandidate(index: Int, editorToken: Long) {
        if (candidateSource != CandidateSource.PREDICTION || index !in candidates.indices) return
        val generation = predictionGeneration
        val result = conversionEngine.commit(index)
        if (generation != predictionGeneration || !editorSession.isCurrent(editorToken)) return
        val committed = result?.value ?: run {
            clearCandidateState()
            return
        }
        expectPredictionSelectionAfterCommit(committed, "")
        textController.commitText(committed)
        clearCandidateState()
        requestNextWordPrediction(editorToken)
    }

    private suspend fun requestNextWordPrediction(editorToken: Long) {
        val context = textController.predictionContext() ?: run {
            clearCandidateState()
            return
        }
        clearCandidateState()
        val generation = predictionGeneration
        predictionRequestInFlight = true
        val state = try {
            conversionEngine.predict(context)
        } finally {
            predictionRequestInFlight = false
        }
        if (generation != predictionGeneration || !editorSession.isCurrent(editorToken) || textController.isPrivateField) return
        applyPrediction(state)
    }

    private suspend fun clearPredictionIfShown() {
        if (candidateSource == CandidateSource.PREDICTION) resetConversion(clearComposing = false)
    }

    private fun expectPredictionSelectionAfterCommit(value: String, replacedText: String) {
        pendingPredictionSelection = ExpectedSelectionTransition(
            replacedLength = replacedText.length,
            replacementLength = value.length,
        )
    }

    private fun isExpectedPredictionSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
    ): Boolean {
        val expected = pendingPredictionSelection ?: return false
        pendingPredictionSelection = null
        return oldSelStart == oldSelEnd && newSelStart == newSelEnd &&
            newSelStart - oldSelStart == expected.replacementLength - expected.replacedLength
    }

    private fun applyPrediction(state: ConversionState) {
        if (state.candidates.isEmpty()) {
            clearCandidateState()
            return
        }
        candidateSource = CandidateSource.PREDICTION
        conversionCandidates = state.candidates
        candidates = state.candidates.map { it.value }
        selectedCandidate = -1
        showCandidateState()
    }

    private fun showConversionPreview(value: String) {
        if (reading.isEmpty()) return
        conversionPreview = value
        textController.replaceComposing(value)
        showCandidateStrip(emptyList(), -1)
        keyboardView?.setCandidates(emptyList(), -1)
    }

    private fun restoreReadingPreview() {
        if (conversionPreview == null) return
        conversionPreview = null
        textController.replaceComposing(reading)
    }

    private fun applyConversion(state: ConversionState) {
        candidateSource = CandidateSource.JAPANESE
        conversionCandidates = state.candidates
        candidates = state.candidates.map { it.value }
        selectedCandidate = state.selectedIndex
        showCandidateStrip(candidates, selectedCandidate)
        keyboardView?.setCandidates(candidates, selectedCandidate)
        keyboardView?.setConversionActive(reading.isNotEmpty())
    }

    private fun invalidateConversion(clearComposing: Boolean) {
        conversionGeneration++
        clearCandidateState()
        val invalidatedEditor = editorSession.capture()
        serviceScope.launch {
            actionMutex.withLock {
                if (editorSession.isCurrent(invalidatedEditor)) resetConversion(clearComposing)
            }
        }
    }

    private suspend fun resetConversion(clearComposing: Boolean) {
        conversionGeneration++
        if (clearComposing) textController.cancelComposition()
        clearCandidateState()
        conversionEngine.reset()
    }

    private fun clearCandidateState() {
        predictionGeneration++
        englishGeneration++
        englishBuffer = ""
        slashGeneration++
        slashBufferActive = false
        reading = ""
        candidates = emptyList()
        conversionCandidates = emptyList()
        selectedCandidate = -1
        conversionPreview = null
        candidateSource = CandidateSource.NONE
        showCandidateStrip(emptyList(), -1)
        keyboardView?.setCandidates(emptyList(), -1)
        keyboardView?.setConversionActive(false)
    }

    internal fun onVoiceState(state: VoiceBackendState, token: Long) {
        if (!editorSession.isCurrent(token) || textController.isPrivateField) return
        if (keyboardMode == KeyboardMode.VOICE) {
            keyboardView?.setVoiceSessionActive(state.isContinuousVoiceSession())
            when (state) {
                is VoiceBackendState.Partial -> {
                    candidateSource = CandidateSource.VOICE
                    candidates = listOf(state.text)
                    showVoiceCandidates(candidates, selectable = false)
                    setVoiceUi(state.toUiState())
                }
                is VoiceBackendState.Preview -> {
                    candidateSource = CandidateSource.VOICE
                    candidates = state.candidates
                    showVoiceCandidates(candidates, selectable = true)
                    setVoiceUi(state.toUiState())
                }
                else -> {
                    candidateSource = CandidateSource.NONE
                    candidates = emptyList()
                    showVoiceCandidates(emptyList(), selectable = false)
                    setVoiceUi(state.toUiState())
                }
            }
            return
        }
        val holdId = voiceHoldRequestId
        if (holdId != null && token == voiceHoldEditorToken) {
            when (state) {
                VoiceBackendState.Recording -> {
                    voiceHoldReady = true
                    keyboardView?.onVoiceRecordingReady(holdId)
                    if (voiceHoldReleased) voiceController.stop()
                }
                is VoiceBackendState.Preview -> {
                    if (!voiceHoldReleased) return
                    val text = voiceController.confirm(token) ?: return
                    commitVoiceHold(holdId, token, text)
                    return
                }
                is VoiceBackendState.Unavailable -> {
                    voiceHoldRequestId = null
                    voiceHoldReady = false
                    voiceHoldReleased = false
                }
                else -> Unit
            }
        }
        setVoiceUi(state.toUiState())
    }

    private fun commitVoiceHold(requestId: Long, token: Long, text: String) {
        if (voiceHoldRequestId != requestId) return
        val generation = voiceHoldGeneration
        voiceHoldRequestId = null
        voiceHoldReady = false
        serviceScope.launch {
            actionMutex.withLock {
                if (generation != voiceHoldGeneration || !editorSession.isCurrent(token) || textController.isPrivateField) return@withLock
                resetConversion(clearComposing = false)
                if (generation != voiceHoldGeneration || !editorSession.isCurrent(token)) return@withLock
                editorSession.runIfCurrent(token) { textController.commitText(text) }
                setVoiceUi(voiceController.initialState().toUiState())
            }
        }
    }

    private fun setVoiceUi(state: VoiceUiState) {
        voicePanel?.setVoiceState(VoiceUiSnapshot(++voiceUiToken, state))
    }

    override fun onDestroy() {
        voiceController.destroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    internal fun installTestDependencies(
        voice: VoiceRecognitionController,
        text: TextInputController,
        conversion: ConversionEngine,
        english: EnglishSuggestionEngine? = null,
    ) {
        voiceController = voice
        textController = text
        testConversionEngine = conversion
        testEnglishSuggestionEngine = english
    }

}

internal fun resolveInitialKeyboardBottomInset(
    metricsBottom: Int?,
    decorBottom: Int,
    legacyBottom: Int,
): Int = metricsBottom
    ?: decorBottom.takeIf { it > 0 }
    ?: legacyBottom

private enum class CandidateSource { NONE, JAPANESE, PREDICTION, ENGLISH, SLASH, VOICE }

private val HISTORY_CANDIDATE_SOURCES = setOf(CandidateSource.JAPANESE, CandidateSource.PREDICTION)

private data class CandidateSnapshot(
    val source: CandidateSource,
    val candidates: List<String>,
    val conversionGeneration: Long,
    val englishGeneration: Long,
    val englishBuffer: String,
    val slashGeneration: Long,
    val slashBufferActive: Boolean,
    val predictionGeneration: Long,
)

private data class ExpectedSelectionTransition(
    val replacedLength: Int,
    val replacementLength: Int,
)

private data class EmojiCategoryTransition(
    val generation: Long,
    val targetCategory: Int,
    val baseline: EmojiViewportBaseline,
)

internal data class EmojiViewportBaseline(
    val viewportHeight: Int?,
    val wasLocked: Boolean,
    val clipBounds: Rect?,
)

private fun Char.isAsciiLetterOrDigit(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

internal fun isEligibleHistoryLongPress(
    event: CandidateUiLongPressEvent,
    currentToken: Long,
    isJapaneseCandidateSource: Boolean,
    conversionCandidates: List<ConversionCandidate>,
): Boolean =
    event.token == currentToken && isJapaneseCandidateSource &&
        conversionCandidates.getOrNull(event.index)?.source == ConversionCandidateSource.MOZC

private const val MAX_ENGLISH_BUFFER = 64
private const val MAX_ENGLISH_CANDIDATES = 5
private const val EMOJI_PICKER_BODY_SPACER_DP = 8f
private const val EMOJI_RECENT_CATEGORY_POSITION = 0
// AndroidX EmojiPicker 1.6.0 ItemType.CATEGORY_TITLE.ordinal. ItemType is Kotlin-internal,
// while RecyclerView.Adapter.getItemViewType() is public.
private const val EMOJI_PICKER_CATEGORY_TITLE_VIEW_TYPE = 0

/** Returns the body-coordinate lower edge of three attached AndroidX emoji rows. */
internal fun emojiThreeRowViewport(body: RecyclerView): Int? {
    val spacer = body.resources.getDimensionPixelSize(androidx.emoji2.emojipicker.R.dimen.emoji_picker_category_name_height)
    return thirdEmojiRowBottomAtCategoryStart(emojiPickerRowBounds(body), spacer)
}

private fun emojiPickerRowBounds(body: RecyclerView): List<Rect> {
    val bounds = mutableListOf<Rect>()
    fun collect(view: View) {
        if (view.javaClass.name == "androidx.emoji2.emojipicker.EmojiView") {
            bounds += Rect(0, 0, view.width, view.height).also { body.offsetDescendantRectToMyCoords(view, it) }
        }
        if (view is ViewGroup) repeat(view.childCount) { collect(view.getChildAt(it)) }
    }
    collect(body)
    return bounds
}

private fun hasVisibleEmojiEmptyCategoryPlaceholder(body: RecyclerView): Boolean {
    val placeholder = body.findViewById<View>(androidx.emoji2.emojipicker.R.id.emoji_picker_empty_category_view)
        ?: return false
    val bounds = emojiEmptyPlaceholderBounds(body) ?: return false
    val viewport = body.clipBounds ?: Rect(0, 0, body.width, body.height)
    return isEmojiPlaceholderInViewport(placeholder.visibility, bounds, viewport)
}

private fun emojiEmptyPlaceholderBounds(body: RecyclerView): Rect? {
    val placeholder = body.findViewById<View>(androidx.emoji2.emojipicker.R.id.emoji_picker_empty_category_view)
        ?: return null
    return Rect(0, 0, placeholder.width, placeholder.height).also {
        body.offsetDescendantRectToMyCoords(placeholder, it)
    }
}

/** Empty Recent occupies one row; retain exactly its placeholder plus two full emoji rows. */
private fun emptyRecentViewport(body: RecyclerView): Int? =
    emptyRecentViewportFromBounds(emojiEmptyPlaceholderBounds(body), emojiPickerRowBounds(body))

internal fun emptyRecentViewportFromBounds(
    placeholderBounds: Rect?,
    emojiBounds: List<Rect>,
    columns: Int = 8,
): Int? {
    val placeholder = placeholderBounds ?: return null
    val rowsAfterPlaceholder = emojiBounds
        .groupBy { it.top }
        .toSortedMap()
        .values
        .filter { row -> row.first().top >= placeholder.bottom && row.size >= columns }
    return rowsAfterPlaceholder.getOrNull(1)?.maxOf { it.bottom }
}

internal fun thirdEmojiRowBottom(bounds: List<Rect>): Int? =
    bounds.groupBy { it.top }.toSortedMap().values.toList().getOrNull(2)?.maxOf { it.bottom }

/** Empty Recent has a placeholder before another category; do not lock on those later rows. */
internal fun thirdEmojiRowBottomAtCategoryStart(bounds: List<Rect>, categorySpacer: Int): Int? {
    val rows = bounds.groupBy { it.top }.toSortedMap().values.toList()
    val firstTop = rows.firstOrNull()?.firstOrNull()?.top ?: return null
    // AndroidX may round a normal category's spacer one pixel past its nominal height.
    // An empty Recent placeholder begins far below this bounded tolerance.
    val categoryStartTolerance = maxOf(categorySpacer * 2, 2)
    return if (firstTop in 0..categoryStartTolerance) rows.getOrNull(2)?.maxOf { it.bottom } else null
}

internal fun resolveEmojiViewport(lockedViewport: Int?, observedViewport: Int?): Int? =
    lockedViewport ?: observedViewport

internal fun resolveEmojiViewportWithPlaceholder(
    lockedViewport: Int?,
    maximumViewport: Int?,
    observedViewport: Int?,
    emptyPlaceholderVisible: Boolean,
): Int? = lockedViewport ?: observedViewport ?: if (emptyPlaceholderVisible) maximumViewport else null

/** The physical keyboard preset caps every category; a prior category's actual height does not. */
internal fun boundedEmojiViewport(maximumViewport: Int?, observedViewport: Int?): Int? =
    observedViewport?.let { observed -> maximumViewport?.let { observed.coerceAtMost(it) } ?: observed }

internal fun isCurrentEmojiCategoryTransition(generation: Long, activeGeneration: Long?): Boolean =
    generation == activeGeneration

/** A rapid B→C activation retains A's settled geometry while C's content is loading. */
internal fun nextEmojiCategoryBaseline(
    pendingBaseline: EmojiViewportBaseline?,
    currentBaseline: EmojiViewportBaseline,
): EmojiViewportBaseline = pendingBaseline ?: currentBaseline

internal fun shouldApplyEmojiPickerViewport(currentBody: RecyclerView?, callbackBody: RecyclerView): Boolean =
    currentBody === callbackBody

internal fun isEmojiCategoryActivationKey(keyCode: Int, action: Int): Boolean =
    action == android.view.KeyEvent.ACTION_UP && keyCode in setOf(
        android.view.KeyEvent.KEYCODE_ENTER,
        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
        android.view.KeyEvent.KEYCODE_SPACE,
    )

internal fun isEmojiCategoryAccessibilityAction(action: Int): Boolean =
    action == AccessibilityNodeInfoCompat.ACTION_CLICK

/** AndroidX recycles header holders; every attachment restores the fixed tap-target width. */
internal fun enforceEmojiCategoryHolderWidth(holder: View, width: Int) {
    if (holder.minimumWidth != width) holder.minimumWidth = width
    val params = holder.layoutParams
    when {
        params == null -> holder.layoutParams = RecyclerView.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT)
        params.width != width -> {
            params.width = width
            holder.layoutParams = params
        }
    }
}

internal fun isEmojiCategoryContentReady(
    targetCategory: Int,
    observedViewport: Int?,
    emptyPlaceholderVisible: Boolean,
): Boolean = observedViewport != null &&
    (!emptyPlaceholderVisible || targetCategory == EMOJI_RECENT_CATEGORY_POSITION)

/** AndroidX scrolls a header selection to its matching category-title adapter item. */
internal fun isEmojiCategoryAtBodyStart(
    firstVisiblePosition: Int,
    categoryTitlePositions: List<Int>,
    targetCategory: Int,
): Boolean = firstVisiblePosition == categoryTitlePositions.getOrNull(targetCategory)

private fun isTargetEmojiCategoryAtBodyStart(body: RecyclerView, targetCategory: Int): Boolean {
    val manager = body.layoutManager as? GridLayoutManager ?: return false
    return isEmojiCategoryAtBodyStart(
        firstVisiblePosition = manager.findFirstVisibleItemPosition(),
        categoryTitlePositions = emojiCategoryTitlePositions(body),
        targetCategory = targetCategory,
    )
}

private fun emojiCategoryTitlePositions(body: RecyclerView): List<Int> {
    val adapter = body.adapter ?: return emptyList()
    return buildList {
        repeat(adapter.itemCount) { position ->
            if (adapter.getItemViewType(position) == EMOJI_PICKER_CATEGORY_TITLE_VIEW_TYPE) add(position)
        }
    }
}

internal fun isEmojiPlaceholderInViewport(visibility: Int, bounds: Rect, viewport: Rect): Boolean =
    visibility == View.VISIBLE && !bounds.isEmpty && Rect.intersects(bounds, viewport)

/** A clipped or partial EmojiView must not remain a TalkBack target. */
internal fun isEmojiCellFullyVisibleInViewport(bounds: Rect, viewport: Rect): Boolean =
    !bounds.isEmpty && viewport.contains(bounds)

internal fun shouldUpdateEmojiPickerMask(
    currentTopMargin: Int,
    currentHeight: Int,
    nextTopMargin: Int,
    nextHeight: Int,
): Boolean = currentTopMargin != nextTopMargin || currentHeight != nextHeight

private fun VoiceBackendState.toUiState(): VoiceUiState = when (this) {
    VoiceBackendState.Idle -> VoiceUiState.Idle
    VoiceBackendState.PermissionRequired -> VoiceUiState.PermissionRequired
    VoiceBackendState.Recording -> VoiceUiState.Recording
    VoiceBackendState.Recognizing -> VoiceUiState.Recognizing
    is VoiceBackendState.Partial -> VoiceUiState.Partial(text)
    is VoiceBackendState.Preview -> VoiceUiState.Preview(candidates.firstOrNull().orEmpty())
    is VoiceBackendState.Unavailable -> VoiceUiState.Unavailable(message)
}

private fun VoiceBackendState.isContinuousVoiceSession(): Boolean = when (this) {
    VoiceBackendState.Recording,
    VoiceBackendState.Recognizing,
    is VoiceBackendState.Partial,
    is VoiceBackendState.Preview,
    -> true
    VoiceBackendState.Idle,
    VoiceBackendState.PermissionRequired,
    is VoiceBackendState.Unavailable,
    -> false
}

internal fun String.toKatakana(): String = map { char ->
    if (char in 'ぁ'..'ゖ') (char.code + 0x60).toChar() else char
}.joinToString("")
