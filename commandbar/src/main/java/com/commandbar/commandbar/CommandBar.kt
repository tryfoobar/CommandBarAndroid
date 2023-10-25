package com.commandbar.commandbar

import android.app.Activity
import android.util.DisplayMetrics
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class CommandBar() {
    fun openHelpHub(activity: Activity) {
        activity.runOnUiThread {
            val webView = HelpHubWebView(activity)
            // Create a bottom sheet modal to hold the WebView
            val bottomSheetDialog = BottomSheetDialog(activity)
            bottomSheetDialog.setContentView(webView)

            // Retrieve the BottomSheetBehavior from the BottomSheetDialog's internal content view
            val bottomSheet =
                bottomSheetDialog.findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenHeight = displayMetrics.heightPixels
            behavior.peekHeight = screenHeight

            bottomSheetDialog.show()
        }
    }
}
