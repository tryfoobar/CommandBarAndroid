package com.commandbar.android

import android.content.Context

object CommandBar {
      private var currentHelpHubWebView: HelpHubWebView? = null

        fun openHelpHub(context: Context, options: CommandBarOptions, articleId: Int? = null, onFallbackAction: FallbackActionCallback? = null) {
                currentHelpHubWebView = HelpHubWebView(context, options, articleId, onFallbackAction)
                currentHelpHubWebView?.openBottomSheetDialog()
        }

        fun closeHelpHub() {
                currentHelpHubWebView?.closeBottomSheetDialog()
                currentHelpHubWebView = null
        }
}
