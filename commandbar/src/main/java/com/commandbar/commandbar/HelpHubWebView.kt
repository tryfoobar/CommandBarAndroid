package com.commandbar.commandbar

import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

// TODO: Pull this out to its own open source package
class HelpHubWebView(context: Context) : WebView(context) {
    init {
        webChromeClient = WebChromeClient()

        // Enable JavaScript in the WebView
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val snippet = getSnippet()
                view?.evaluateJavascript(snippet, null)
            }
        }

        val html = getHTML()
        loadDataWithBaseURL("http://api.commandbar.com", html, "text/html", "UTF-8", null)
    }

    companion object {

        fun getSnippet(): String {
            // TODO: Make these configurable
            val orgId = "foocorp"
            val launchCode = "local-dev"
            val userId = "none"
            val hostname = "10.0.2.2"
            return """
              (function() {
                  var o="$orgId",n=["Object.assign","Symbol","Symbol.for"].join("%2C"),a=window;function t(o,n){void 0===n&&(n=!1),"complete"!==document.readyState&&window.addEventListener("load",t.bind(null,o,n),{capture:!1,once:!0});var a=document.createElement("script");a.type="text/javascript",a.async=n,a.src=o,document.head.appendChild(a)}function r(){var n;if(void 0===a.CommandBar){delete a.__CommandBarBootstrap__;var r=Symbol.for("CommandBar::configuration"),e=Symbol.for("CommandBar::orgConfig"),c=Symbol.for("CommandBar::disposed"),i=Symbol.for("CommandBar::isProxy"),m=Symbol.for("CommandBar::queue"),l=Symbol.for("CommandBar::unwrap"),d=[],s="$launchCode",u=s&&s.includes("local")?"http://$hostname:8000":"https://api.commandbar.com",f=Object.assign(((n={})[r]={uuid:o},n[e]={},n[c]=!1,n[i]=!0,n[m]=new Array,n[l]=function(){return f},n),a.CommandBar),p=["addCommand","boot"],y=f;Object.assign(f,{shareCallbacks:function(){return{}},shareContext:function(){return{}}}),a.CommandBar=new Proxy(f,{get:function(o,n){return n in y?f[n]:p.includes(n)?function(){var o=Array.prototype.slice.call(arguments);return new Promise((function(a,t){o.unshift(n,a,t),f[m].push(o)}))}:function(){var o=Array.prototype.slice.call(arguments);o.unshift(n),f[m].push(o)}}}),null!==s&&d.push("lc=".concat(s)),d.push("version=2"),t("".concat(u,"/latest/").concat(o,"?").concat(d.join("&")),!0)}}void 0===Object.assign||"undefined"==typeof Symbol||void 0===Symbol.for?(a.__CommandBarBootstrap__=r,t("https://polyfill.io/v3/polyfill.min.js?version=3.101.0&callback=__CommandBarBootstrap__&features="+n)):r();
                  window.CommandBar.boot("$userId", { products: ["help_hub"] });
                  window.CommandBar.openHelpHub();
              })();
          """
        }

        @JvmStatic()
        fun getHTML(): String {
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
