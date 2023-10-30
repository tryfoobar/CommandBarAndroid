package com.commandbar.android

import android.content.Context

object CommandBar {
        fun openHelpHub(context: Context, options: CommandBarOptions) {
                val webView = HelpHubWebView(context, options)
                webView.openBottomSheetDialog()
        }
}
