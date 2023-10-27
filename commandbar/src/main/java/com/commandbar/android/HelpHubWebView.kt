package com.commandbar.android

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetDialog

class HelpHubWebView(context: Context, orgId: String?) : WebView(context) {
    private lateinit var orgId: String;

    init {
        webChromeClient = WebChromeClient()
        webChromeClient = WebChromeClient()

        // Enable JavaScript in the WebView
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        if (orgId != null) {
            this.orgId = orgId
            setupWebView(this.orgId)
        }
    }

    fun setOrgId(orgId: String) {
        this.orgId = orgId
        setupWebView(orgId)
    }

    fun openBottomSheetDialog() {
        // Create the BottomSheetDialog
        var dialog = BottomSheetDialog(context)
        val coordinatorLayout = CoordinatorLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(this@HelpHubWebView)
        }

        dialog.setContentView(coordinatorLayout)

        // Adjust the height of the dialog to match the screen height
        val windowHeight = Resources.getSystem().displayMetrics.heightPixels
        dialog.behavior.peekHeight = windowHeight


        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet);
        // Show the dialog using the post method to wait for the view to be fully measured and laid out
        dialog.show()

        if (bottomSheet != null) {
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    private fun setupWebView (orgId: String) {

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val snippet = getSnippet(orgId)
                view?.evaluateJavascript(snippet) {
                    println(it)
                }
            }
        }

        val html = getHTML()
        loadDataWithBaseURL("https://api.commandbar.com", html, "text/html", "UTF-8", null)
    }

    companion object {

        @JvmStatic
        private fun getSnippet(orgId: String): String {
            val launchCode = "labs"
            val hostname = "10.0.2.2"
            val apiHost = "api.commandbar.com"
            return """
              (function() {
                  var o="$orgId",n=["Object.assign","Symbol","Symbol.for"].join("%2C"),a=window;function t(o,n){void 0===n&&(n=!1),"complete"!==document.readyState&&window.addEventListener("load",t.bind(null,o,n),{capture:!1,once:!0});var a=document.createElement("script");a.type="text/javascript",a.async=n,a.src=o,document.head.appendChild(a)}function r(){var n;if(void 0===a.CommandBar){delete a.__CommandBarBootstrap__;var r=Symbol.for("CommandBar::configuration"),e=Symbol.for("CommandBar::orgConfig"),c=Symbol.for("CommandBar::disposed"),i=Symbol.for("CommandBar::isProxy"),m=Symbol.for("CommandBar::queue"),l=Symbol.for("CommandBar::unwrap"),d=[],s="$launchCode",u=s&&s.includes("local")?"http://$hostname:8000":"https://$apiHost",f=Object.assign(((n={})[r]={uuid:o},n[e]={},n[c]=!1,n[i]=!0,n[m]=new Array,n[l]=function(){return f},n),a.CommandBar),p=["addCommand","boot"],y=f;Object.assign(f,{shareCallbacks:function(){return{}},shareContext:function(){return{}}}),a.CommandBar=new Proxy(f,{get:function(o,n){return n in y?f[n]:p.includes(n)?function(){var o=Array.prototype.slice.call(arguments);return new Promise((function(a,t){o.unshift(n,a,t),f[m].push(o)}))}:function(){var o=Array.prototype.slice.call(arguments);o.unshift(n),f[m].push(o)}}}),null!==s&&d.push("lc=".concat(s)),d.push("version=2"),t("".concat(u,"/latest/").concat(o,"?").concat(d.join("&")),!0)}}void 0===Object.assign||"undefined"==typeof Symbol||void 0===Symbol.for?(a.__CommandBarBootstrap__=r,t("https://polyfill.io/v3/polyfill.min.js?version=3.101.0&callback=__CommandBarBootstrap__&features="+n)):r();
                  window.CommandBar.boot(null, { products: ["help_hub"] });
                  window.CommandBar.openHelpHub();
              })();
          """
        }

        @JvmStatic()
        private  fun getHTML(): String {
            return """
            <!DOCTYPE html>
            <html>
                <head>
                    <meta name="viewport" content="user-scalable=no, width=device-width, height=device-height, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no">
                    <style>
                        html, body {
                          height: 100%;
                        }
                        #helphub-close-button {
                            display: none !important;
                        }

                        #copilot-container:not(:focus-within) {
                            padding-bottom: 50px;
                        }
                    </style>
                </head>
                <body>
                    <div></div>
                </body>
            </html>
        """.trimIndent()
        }
    }
}
