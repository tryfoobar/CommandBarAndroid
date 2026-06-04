package com.commandbar.android

import android.content.Context
import org.json.JSONObject

object CommandBar {
      private var currentResourceCenterWebView: ResourceCenterWebView? = null

        fun openResourceCenter(
            context: Context,
            options: CommandBarOptions,
            articleId: Int? = null,
            onFallbackAction: FallbackActionCallback? = null,
        ) {
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

        fun openAssistant(
            context: Context,
            options: CommandBarOptions,
            onFallbackAction: FallbackActionCallback? = null,
        ) {
            currentResourceCenterWebView = ResourceCenterWebView(
                context,
                options,
                articleId = null,
                onFallbackAction,
                engagementShell = "assistant",
            )
            currentResourceCenterWebView?.openBottomSheetDialog()
        }

        fun closeResourceCenter() {
            currentResourceCenterWebView?.closeEngagementShell()
            currentResourceCenterWebView?.closeBottomSheetDialog()
            currentResourceCenterWebView = null
        }

        /** Mirrors `window.engagement.assistant.setAssistantFilter`. Pass null to clear. */
        fun setAssistantFilter(filter: JSONObject?) {
            EngagementFilterStore.setAssistantFilter(filter)
            currentResourceCenterWebView?.applyEngagementFilters()
        }

        /** Mirrors `window.engagement.setResourceCenterFilter`. Pass null to clear. */
        fun setResourceCenterFilter(filter: JSONObject?) {
            EngagementFilterStore.setResourceCenterFilter(filter)
            currentResourceCenterWebView?.applyEngagementFilters()
        }
}
