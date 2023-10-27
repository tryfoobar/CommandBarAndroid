package com.commandbar.android

import android.content.Context

object CommandBar {
        fun openHelpHub(context: Context, orgId: String) {
                val webView = HelpHubWebView(context, orgId)
                webView.openBottomSheetDialog()
        }
}
