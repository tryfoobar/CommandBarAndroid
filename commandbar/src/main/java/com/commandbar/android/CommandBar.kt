package com.commandbar.android

import android.content.Context

object CommandBar {
        fun openHelpHub(context: Context, options: CommandBarOptions, articleId: Int? = null, onFallbackAction: FallbackActionCallback? = null) {
                val webView = HelpHubWebView(context, options, articleId, onFallbackAction)
                webView.openBottomSheetDialog()
        }
}
