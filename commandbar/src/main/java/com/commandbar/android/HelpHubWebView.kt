package com.commandbar.android

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.commandbar.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject


typealias FallbackActionCallback = ((action: Map<String, Any>) -> Unit)

fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

fun Context.pxToDp(px: Int): Float {
    return (px.toFloat() / resources.displayMetrics.density)
}

class HelpHubWebView(context: Context, options: CommandBarOptions? = null, articleId: Int? = null, onFallbackAction: FallbackActionCallback? = null) : WebView(context) {
    private lateinit var options: CommandBarOptions;
    private lateinit var onFallbackAction: FallbackActionCallback
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var articleId: Int?

    init {
        this.articleId = articleId
        if (onFallbackAction != null) {
            this.onFallbackAction = onFallbackAction
            resetJavascriptInterface(onFallbackAction)
        }

        if (options != null) {
            this.options = options
            setupWebView(options, this.articleId)
        }


    }

    fun setOptions(options: CommandBarOptions) {
        this.options = options
        setupWebView(options)
    }

    fun setFallbackActionCallback(callback: FallbackActionCallback) {
        this.onFallbackAction = callback
        resetJavascriptInterface(callback)
    }

    inner class CommandBarJavaScriptInterface(private var callback: FallbackActionCallback? = null) {
        @JavascriptInterface
        fun commandbar__onFallbackAction(action: String?) {
            if (callback != null) {
                val actionParam = if (action != null) JSONObject(action).toMap() else emptyMap()
                callback?.let { it(actionParam) }
            }
        }

        private fun JSONObject.toMap(): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            val keysIterator = keys()

            while (keysIterator.hasNext()) {
                val key = keysIterator.next()
                val value = this[key]

                if (value is JSONObject) {
                    map[key] = value.toMap() // Recursively convert nested JSON objects
                } else {
                    map[key] = value
                }
            }

            return map
        }
    }

    private fun resetJavascriptInterface(callback: FallbackActionCallback) {
        try {
            removeJavascriptInterface("CommandBarAndroidInterface")
        } catch (e: Exception) {
            // Do nothing
        } finally {
            addJavascriptInterface(CommandBarJavaScriptInterface(callback), "CommandBarAndroidInterface")
        }
    }

    private fun setupWebView (options: CommandBarOptions, articleId: Int? = null) {
        webChromeClient = WebChromeClient()
        setBackgroundColor(Color.TRANSPARENT)

        // Enable JavaScript in the WebView
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        if (options.launchCode == "local") {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }


        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(
                    "(function() { return !!window.CommandBar && !!window.CommandBar.openHelpHub; })();")
                    {
                        if (it != "true") {
                            val snippet = getSnippet(options, articleId)
                            view?.evaluateJavascript(snippet) {
                                println(it)
                            }
                        }
                    }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, request!!.url)
                view!!.context.startActivity(intent)
                return true;
            }
        }

        val html = getHTML(options)
        loadDataWithBaseURL("https://api.commandbar.com", html, "text/html", "UTF-8", null)
    }

    fun openBottomSheetDialog() {
        // Create the BottomSheetDialog
        bottomSheetDialog = BottomSheetDialog(context, R.style.HelpHubBottomSheet)
        val coordinatorLayout = CoordinatorLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(this@HelpHubWebView)
        }

        bottomSheetDialog?.setContentView(coordinatorLayout)

        // Adjust the height of the dialog to match the screen height
        val windowHeight = Resources.getSystem().displayMetrics.heightPixels
        val sheetHeight = windowHeight - context.dpToPx(40)
        bottomSheetDialog?.behavior?.peekHeight = sheetHeight
        this.layoutParams.height = sheetHeight
        bottomSheetDialog?.behavior?.maxHeight = sheetHeight

        val bottomSheet = bottomSheetDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet);
        // Show the dialog using the post method to wait for the view to be fully measured and laid out
        bottomSheetDialog?.show()

        if (bottomSheet != null) {
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    fun closeBottomSheetDialog() {
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
        this.destroy()
    }

    companion object {

        @JvmStatic
        private fun getSnippet(options: CommandBarOptions, articleId: Int? = null): String {
            val hostname = "10.0.2.2"
            val apiHost = "api.commandbar.com";
            val userId = if (options.userId == null) "null" else "\"${options.userId}\""
            val launchCode = if (options.launchCode == null) "prod" else options.launchCode

            val executable = if (articleId == null) "window.CommandBar.openHelpHub()" else "window.CommandBar.openHelpHub({ articleId: $articleId })"
            return """
              (function() {
                  window._cbIsWebView = true;
                  var o="${options.orgId}",n=["Object.assign","Symbol","Symbol.for"].join("%2C"),a=window;function t(o,n){void 0===n&&(n=!1),"complete"!==document.readyState&&window.addEventListener("load",t.bind(null,o,n),{capture:!1,once:!0});var a=document.createElement("script");a.type="text/javascript",a.async=n,a.src=o,document.head.appendChild(a)}function r(){var n;if(void 0===a.CommandBar){delete a.__CommandBarBootstrap__;var r=Symbol.for("CommandBar::configuration"),e=Symbol.for("CommandBar::orgConfig"),c=Symbol.for("CommandBar::disposed"),i=Symbol.for("CommandBar::isProxy"),m=Symbol.for("CommandBar::queue"),l=Symbol.for("CommandBar::unwrap"),d=[],s="api=$launchCode;commandbar=$launchCode",u=s&&s.includes("local")?"http://$hostname:8000":"https://$apiHost",f=Object.assign(((n={})[r]={uuid:o},n[e]={},n[c]=!1,n[i]=!0,n[m]=new Array,n[l]=function(){return f},n),a.CommandBar),p=["addCommand","boot"],y=f;Object.assign(f,{shareCallbacks:function(){return{}},shareContext:function(){return{}}}),a.CommandBar=new Proxy(f,{get:function(o,n){return n in y?f[n]:p.includes(n)?function(){var o=Array.prototype.slice.call(arguments);return new Promise((function(a,t){o.unshift(n,a,t),f[m].push(o)}))}:function(){var o=Array.prototype.slice.call(arguments);o.unshift(n),f[m].push(o)}}}),null!==s&&d.push("lc=".concat(s)),d.push("version=2"),t("".concat(u,"/latest/").concat(o,"?").concat(d.join("&")),!0)}}void 0===Object.assign||"undefined"==typeof Symbol||void 0===Symbol.for?(a.__CommandBarBootstrap__=r,t("https://polyfill.io/v3/polyfill.min.js?version=3.101.0&callback=__CommandBarBootstrap__&features="+n)):r();
                  window.CommandBar.boot($userId, {}, { products: ["help_hub"] });
                  console.log("Rerunning executable");
                  $executable
              })();
          """
        }

        @JvmStatic()
        private  fun getHTML(options: CommandBarOptions): String {
            return """
            <!DOCTYPE html>
            <html>
              <head>
                  <meta name="viewport" content="user-scalable=no, width=device-width, height=device-height, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no">
                  <style>
                      .loading-container {
                          display: flex;
                          justify-content: center;
                          align-items: center;
                          width: 100%;
                          height: 100%;
                      }
                      .lds-ring {
                        display: inline-block;
                        position: relative;
                        width: 80px;
                        height: 80px;
                      }
                      .lds-ring div {
                        box-sizing: border-box;
                        display: block;
                        position: absolute;
                        width: 64px;
                        height: 64px;
                        margin: 8px;
                        border: 8px solid ${options.spinnerColor};
                        border-radius: 50%;
                        animation: lds-ring 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
                        border-color: ${options.spinnerColor} transparent transparent transparent;
                      }
                      .lds-ring div:nth-child(1) {
                        animation-delay: -0.45s;
                      }
                      .lds-ring div:nth-child(2) {
                        animation-delay: -0.3s;
                      }
                      .lds-ring div:nth-child(3) {
                        animation-delay: -0.15s;
                      }
                      @keyframes lds-ring {
                        0% {
                          transform: rotate(0deg);
                        }
                        100% {
                          transform: rotate(360deg);
                        }
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
                  <div class="loading-container"><div class="lds-ring"><div></div><div></div><div></div><div></div></div></div>
              </body>
            </html>
        """.trimIndent()
        }
    }
}
