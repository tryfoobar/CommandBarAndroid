package com.commandbar.android

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.util.Log
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
import java.util.Locale


typealias FallbackActionCallback = ((action: Map<String, Any>) -> Unit)

fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

fun Context.pxToDp(px: Int): Float {
    return (px.toFloat() / resources.displayMetrics.density)
}

class ResourceCenterWebView(
    context: Context,
    options: CommandBarOptions? = null,
    articleId: Int? = null,
    onFallbackAction: FallbackActionCallback? = null,
    private val engagementInitialPage: String = "help-hub",
) : WebView(context) {
    private lateinit var options: CommandBarOptions
    private var onFallbackAction: FallbackActionCallback = onFallbackAction ?: { }
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var articleId: Int?

    init {
        this.articleId = articleId
        resetJavascriptInterface(this.onFallbackAction)

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

    inner class CommandBarJavaScriptInterface(private var callback: FallbackActionCallback) {
        @JavascriptInterface
        fun onResourceCenterClose() {
            post {
                CommandBar.closeResourceCenter()
            }
        }

        @JavascriptInterface
        fun commandbar__onFallbackAction(action: String?) {
            val actionParam = if (action != null) JSONObject(action).toMap() else emptyMap()
            val meta = actionParam["meta"] as? Map<*, *>
            val type = meta?.get("type") as? String
            post {
                if (type == "close") {
                    CommandBar.closeResourceCenter()
                }
                callback(actionParam)
            }
        }

        /** Mirrors iOS `engagement__log` — injected JS uses this for the same diagnostics as WKWebView. */
        @JavascriptInterface
        fun engagement__log(msg: String?) {
            if (!msg.isNullOrEmpty()) {
                Log.d(TAG_RESOURCE_CENTER_WEB_VIEW, msg)
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
                val webView = view ?: return
                webView.evaluateJavascript(
                    "(function() { return !!window.__ampMobileResourceCenterLoaded; })();")
                    { result ->
                        if (!isEvaluateJavascriptTruthy(result)) {
                            val snippet = getSnippet(options, articleId, engagementInitialPage)
                            webView.evaluateJavascript(snippet) { }
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
        loadDataWithBaseURL("https://cdn.amplitude.com", html, "text/html", "UTF-8", null)
    }

    fun openBottomSheetDialog() {
        // Create the BottomSheetDialog
        bottomSheetDialog = BottomSheetDialog(context, R.style.ResourceCenterBottomSheet)
        val coordinatorLayout = CoordinatorLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(this@ResourceCenterWebView)
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

    /** Applies the latest native tag filters to a booted engagement instance. */
    fun applyEngagementFilters() {
        evaluateJavascript(buildApplyEngagementFiltersJavaScript()) { }
    }

    private fun isEvaluateJavascriptTruthy(result: String?): Boolean {
        val trimmed = result?.trim() ?: return false
        return trimmed == "true" || trimmed == "\"true\""
    }

    companion object {
        internal const val TAG_RESOURCE_CENTER_WEB_VIEW = "ResourceCenterWebView"

        @JvmStatic
        fun buildApplyEngagementFiltersJavaScript(): String {
            val rc = EngagementFilterStore.resourceCenterFilterJsonLiteral()
            val assistant = EngagementFilterStore.assistantFilterJsonLiteral()
            return """
                (function() {
                    try {
                        if ($rc != null && window.engagement && typeof window.engagement.setResourceCenterFilter === "function") {
                            window.engagement.setResourceCenterFilter($rc);
                        }
                    } catch (e1) {}
                    try {
                        if ($assistant != null && window.engagement && window.engagement.assistant && typeof window.engagement.assistant.setAssistantFilter === "function") {
                            window.engagement.assistant.setAssistantFilter($assistant);
                        }
                    } catch (e2) {}
                })();
            """.trimIndent()
        }

        @JvmStatic
        private fun getSnippet(
            options: CommandBarOptions,
            articleId: Int? = null,
            engagementInitialPage: String = "help-hub",
        ): String {
            val apiKey = JSONObject.quote(options.orgId)
            val userIdRaw = options.userId?.let { JSONObject.quote(it) } ?: "null"
            val articleIdJs = articleId?.toString() ?: "null"
            val engagementInitialPageJs = JSONObject.quote(engagementInitialPage)
            val launchCode = JSONObject.quote(options.launchCode ?: "prod")
            val zone =
                if ((options.serverZone ?: "US").uppercase(Locale.US) == "EU") "EU" else "US"
            val serverZone = JSONObject.quote(zone)
            val localDevHost = JSONObject.quote("10.0.2.2")
            val resourceCenterFilterJs = EngagementFilterStore.resourceCenterFilterJsonLiteral()
            val assistantFilterJs = EngagementFilterStore.assistantFilterJsonLiteral()
            return """
              (function() {
                  window._cbIsWebView = true;

                  function ampLog(msg) {
                      try {
                          if (window.CommandBarAndroidInterface && typeof window.CommandBarAndroidInterface.engagement__log === "function") {
                              window.CommandBarAndroidInterface.engagement__log(String(msg));
                          }
                      } catch (e) {}
                      try { console.log(msg); } catch (e2) {}
                  }

                  function notifyNativeResourceCenterClosed() {
                      try {
                          if (window.CommandBarAndroidInterface && typeof window.CommandBarAndroidInterface.onResourceCenterClose === "function") {
                              window.CommandBarAndroidInterface.onResourceCenterClose();
                              return;
                          }
                      } catch (eDedicated) {}
                      var payload = JSON.stringify({ meta: { type: "close" } });
                      try {
                          if (window.CommandBarAndroidInterface && typeof window.CommandBarAndroidInterface.commandbar__onFallbackAction === "function") {
                              window.CommandBarAndroidInterface.commandbar__onFallbackAction(payload);
                          }
                      } catch (eFallback) {}
                  }

                  function isResourceCenterCloseElement(el) {
                      if (!el || typeof el.getAttribute !== "function") { return false; }
                      var tag = (el.tagName || "").toUpperCase();
                      var role = (el.getAttribute("role") || "").toLowerCase();
                      var label = el.getAttribute("aria-label") || "";
                      if (!/^close$/i.test(label)) { return false; }
                      return tag === "BUTTON" || role === "button";
                  }

                  function eventIndicatesResourceCenterClose(ev) {
                      var path = typeof ev.composedPath === "function" ? ev.composedPath() : [];
                      var i = 0;
                      for (i = 0; i < path.length; i++) {
                          if (isResourceCenterCloseElement(path[i])) { return true; }
                      }
                      var t = ev.target;
                      if (t && typeof t.closest === "function") {
                          if (t.closest('button[aria-label="close"], button[aria-label="Close"], [role="button"][aria-label="close"], [role="button"][aria-label="Close"]')) {
                              return true;
                          }
                      }
                      return false;
                  }

                  function installNativeResourceCenterCloseBridge() {
                      if (window.__ampNativeResourceCenterCloseBridge) { return; }
                      window.__ampNativeResourceCenterCloseBridge = true;
                      var fired = false;
                      function relay(ev) {
                          if (fired || !eventIndicatesResourceCenterClose(ev)) { return; }
                          fired = true;
                          notifyNativeResourceCenterClosed();
                      }
                      document.addEventListener("pointerdown", relay, true);
                      document.addEventListener("click", relay, true);
                      document.addEventListener("touchend", relay, true);
                  }
                  installNativeResourceCenterCloseBridge();

                  if (window.__ampMobileResourceCenterLoaded) { return; }

                  var apiKey = $apiKey;
                  var serverZone = $serverZone;
                  var userIdRaw = $userIdRaw;
                  var articleId = $articleIdJs;
                  var launchCode = $launchCode;
                  var localDevHost = $localDevHost;
                  var engagementInitialPage = $engagementInitialPageJs;
                  var nativeResourceCenterFilter = $resourceCenterFilterJs;
                  var nativeAssistantFilter = $assistantFilterJs;

                  function applyNativeEngagementFilters() {
                      try {
                          if (nativeResourceCenterFilter != null && window.engagement && typeof window.engagement.setResourceCenterFilter === "function") {
                              window.engagement.setResourceCenterFilter(nativeResourceCenterFilter);
                          }
                      } catch (eRc) {}
                      try {
                          if (nativeAssistantFilter != null && window.engagement && window.engagement.assistant && typeof window.engagement.assistant.setAssistantFilter === "function") {
                              window.engagement.assistant.setAssistantFilter(nativeAssistantFilter);
                          }
                      } catch (eAsst) {}
                  }

                  function loadScript(src, async, onload, onerror) {
                      var s = document.createElement("script");
                      s.type = "text/javascript";
                      s.async = !!async;
                      s.src = src;
                      if (onload) { s.onload = onload; }
                      if (onerror) { s.onerror = onerror; }
                      document.head.appendChild(s);
                  }

                  function withPolyfills(done) {
                      var feats = ["Object.assign","Symbol","Symbol.for"].join("%2C");
                      if (typeof Object.assign !== "undefined" && typeof Symbol !== "undefined" && Symbol.for) {
                          done();
                      } else {
                          window.__AmpEngagementPolyDone__ = done;
                          loadScript("https://polyfill.io/v3/polyfill.min.js?version=3.101.0&callback=__AmpEngagementPolyDone__&features=" + feats, false, null, null);
                      }
                  }

                  function engagementScriptUrl() {
                      var cdnBase = serverZone === "EU" ? "https://cdn.eu.amplitude.com" : "https://cdn.amplitude.com";
                      return cdnBase + "/script/" + encodeURIComponent(apiKey) + ".engagement.js";
                  }

                  function engagementInitOptions() {
                      var o = { serverZone: serverZone === "EU" ? "EU" : "US" };
                      if (launchCode === "local") {
                          o.serverUrl = "http://" + localDevHost + ":8000";
                          o.cdnUrl = "http://" + localDevHost + ":8000";
                      }
                      return o;
                  }

                  function buildUser() {
                      if (userIdRaw) {
                          return { user_id: userIdRaw };
                      }
                      try {
                          var k = "__amp_engagement_wv_device";
                          var id = sessionStorage.getItem(k);
                          if (!id) {
                              id = (window.crypto && window.crypto.randomUUID) ? window.crypto.randomUUID() : ("wv-" + Date.now() + "-" + Math.random());
                              sessionStorage.setItem(k, id);
                          }
                          return { device_id: id };
                      } catch (e) {
                          return { device_id: "wv-" + Date.now() + "-" + Math.random() };
                      }
                  }

                  function hideNativeLoadingSpinner() {
                      var els = document.querySelectorAll(".loading-container");
                      for (var i = 0; i < els.length; i++) {
                          els[i].style.display = "none";
                      }
                  }

                  function openResourceCenter() {
                      hideNativeLoadingSpinner();
                      var opts = { initialPage: engagementInitialPage };
                      if (articleId !== null && articleId !== undefined) {
                          opts.contentItemId = articleId;
                      }
                      try {
                          window.dispatchEvent(new Event("resize"));
                      } catch (e1) {}
                      requestAnimationFrame(function () {
                          requestAnimationFrame(function () {
                              try {
                                  window.engagement._showResourceCenter(true, opts);
                                  try {
                                      window.dispatchEvent(new Event("resize"));
                                  } catch (e2) {}
                              } catch (e3) {
                                  ampLog("[Amplitude Engagement] _showResourceCenter: " + e3);
                              }
                              window.__ampMobileResourceCenterLoaded = true;
                          });
                      });
                  }

                  function tryBootAfterEngagementScript() {
                      var attempts = 0;
                      var maxAttempts = 120;
                      function tick() {
                          attempts++;
                          try {
                              if (!window.engagement || typeof window.engagement.boot !== "function") {
                                  if (attempts >= maxAttempts) {
                                      ampLog("[Amplitude Engagement] Timed out waiting for engagement SDK after script load. URL: " + engagementScriptUrl());
                                      return;
                                  }
                                  setTimeout(tick, 100);
                                  return;
                              }
                              window.engagement.init(apiKey, engagementInitOptions());
                              var p = window.engagement.boot({
                                  user: buildUser(),
                                  integrations: [{ track: function () {} }]
                              });
                              if (p && typeof p.then === "function") {
                                  p.then(function () {
                                      applyNativeEngagementFilters();
                                      openResourceCenter();
                                  }).catch(function (err) {
                                      ampLog("[Amplitude Engagement] boot failed: " + err);
                                  });
                              } else {
                                  applyNativeEngagementFilters();
                                  openResourceCenter();
                              }
                          } catch (e) {
                              ampLog("[Amplitude Engagement] boot setup failed: " + e);
                          }
                      }
                      tick();
                  }

                  function start() {
                      var src = engagementScriptUrl();
                      loadScript(src, false, function () {
                          tryBootAfterEngagementScript();
                      }, function () {
                          ampLog("[Amplitude Engagement] Failed to load script: " + src);
                      });
                  }

                  withPolyfills(start);
              })();
          """.trimIndent()
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

                      html, body {
                          margin: 0;
                          padding: 0;
                          width: 100%;
                          height: 100%;
                          min-height: 100%;
                          background-color: #ffffff;
                      }
                      .loading-container {
                          position: fixed;
                          inset: 0;
                          z-index: 1;
                          pointer-events: none;
                          background-color: transparent;
                      }
                      #engagement-wrapper {
                          position: relative;
                          z-index: 2147483000;
                          min-height: 100%;
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
