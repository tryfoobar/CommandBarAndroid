package com.commandbar.android

import android.content.Context

object CommandBar {
        fun openHelpHub(context: Context, options: CommandBarOptions, onFallbackAction: FallbackActionCallback? = null) {
                val webView = HelpHubWebView(context, options, onFallbackAction)
                webView.openBottomSheetDialog()
        }
}
