package com.commandbar.android

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * Public entry point for the Amplitude Engagement WebView SDK on Android.
 *
 * Lifecycle:
 * 1. Call [boot] once at app start with your [CommandBarOptions].
 * 2. Call [openResourceCenter] / [openAssistant] later — they use the booted options.
 */
object CommandBar {
    private const val TAG = "CommandBar"

    private var currentResourceCenterWebView: ResourceCenterWebView? = null

    /** Configuration stored by the most recent [boot] call. Used by every subsequent open call. */
    @JvmStatic
    @Volatile
    var bootOptions: CommandBarOptions? = null
        private set

    /** Stores the configuration used by subsequent [openResourceCenter] / [openAssistant] calls. */
    @JvmStatic
    fun boot(options: CommandBarOptions) {
        bootOptions = options
    }

    @JvmStatic
    @JvmOverloads
    fun openResourceCenter(
        context: Context,
        articleId: Int? = null,
        onFallbackAction: FallbackActionCallback? = null,
    ) {
        val options = bootOptions ?: run {
            Log.w(TAG, "openResourceCenter called before boot(options); no-op.")
            return
        }
        // Only one engagement sheet may exist at a time. Dismiss any currently presented
        // Resource Center / Assistant before opening a new one so repeated or interleaved open
        // calls cannot stack a second WebView/sheet.
        dismissCurrentEngagementSheet()
        currentResourceCenterWebView = ResourceCenterWebView(
            context,
            options,
            articleId,
            onFallbackAction,
            engagementShell = "resource-center",
            engagementInitialPage = "help-hub",
        )
        currentResourceCenterWebView?.openBottomSheetDialog()
    }

    @JvmStatic
    @JvmOverloads
    fun openAssistant(
        context: Context,
        onFallbackAction: FallbackActionCallback? = null,
    ) {
        val options = bootOptions ?: run {
            Log.w(TAG, "openAssistant called before boot(options); no-op.")
            return
        }
        // Only one engagement sheet may exist at a time. Dismiss any currently presented
        // Resource Center / Assistant before opening a new one so repeated or interleaved open
        // calls cannot stack a second WebView/sheet.
        dismissCurrentEngagementSheet()
        currentResourceCenterWebView = ResourceCenterWebView(
            context,
            options,
            articleId = null,
            onFallbackAction,
            engagementShell = "assistant",
        )
        currentResourceCenterWebView?.openBottomSheetDialog()
    }

    /** Dismisses and tears down the currently presented engagement WebView, if any. */
    private fun dismissCurrentEngagementSheet() {
        currentResourceCenterWebView?.closeBottomSheetDialog()
        currentResourceCenterWebView = null
    }

    /**
     * Dismisses the presented engagement sheet (Resource Center or Assistant), if any.
     * The injected WebView bridge picks the correct web-side teardown based on the
     * active shell, so this method works regardless of which `open*` was used.
     */
    @JvmStatic
    fun closeResourceCenter() {
        currentResourceCenterWebView?.closeEngagementShell()
        currentResourceCenterWebView?.closeBottomSheetDialog()
        currentResourceCenterWebView = null
    }

    /**
     * Symmetric alias for [closeResourceCenter]. Both methods dismiss whichever
     * engagement sheet (Resource Center or Assistant) is currently presented.
     */
    @JvmStatic
    fun closeAssistant() {
        closeResourceCenter()
    }

    /** Mirrors `window.engagement.assistant.setAssistantFilter`. Pass null to clear. */
    @JvmStatic
    fun setAssistantFilter(filter: JSONObject?) {
        EngagementFilterStore.setAssistantFilter(filter)
        // Route to the currently attached WebView, which may be either a modal opened
        // via `openResourceCenter` / `openAssistant` or an inline `ResourceCenterView`
        // mounted from React Native. `ResourceCenterWebView` self-registers in its
        // constructor, so this works for both surfaces.
        ResourceCenterWebView.getActiveInstance()?.applyEngagementFilters()
    }

    /** Mirrors `window.engagement.setResourceCenterFilter`. Pass null to clear. */
    @JvmStatic
    fun setResourceCenterFilter(filter: JSONObject?) {
        EngagementFilterStore.setResourceCenterFilter(filter)
        ResourceCenterWebView.getActiveInstance()?.applyEngagementFilters()
    }
}
