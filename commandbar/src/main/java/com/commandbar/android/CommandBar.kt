package com.commandbar.android

import android.content.Context

object CommandBar {
      private var currentHelpHubWebView: HelpHubWebView? = null

        fun openResourceCenter(
            context: Context,
            options: CommandBarOptions,
            articleId: Int? = null,
            onFallbackAction: FallbackActionCallback? = null,
        ) {
            currentHelpHubWebView = HelpHubWebView(
                context,
                options,
                articleId,
                onFallbackAction,
                engagementInitialPage = "help-hub",
            )
            currentHelpHubWebView?.openBottomSheetDialog()
        }

        fun openAssistant(
            context: Context,
            options: CommandBarOptions,
            onFallbackAction: FallbackActionCallback? = null,
        ) {
            currentHelpHubWebView = HelpHubWebView(
                context,
                options,
                articleId = null,
                onFallbackAction,
                engagementInitialPage = "assistant",
            )
            currentHelpHubWebView?.openBottomSheetDialog()
        }

        fun closeResourceCenter() {
            currentHelpHubWebView?.closeBottomSheetDialog()
            currentHelpHubWebView = null
        }
}
