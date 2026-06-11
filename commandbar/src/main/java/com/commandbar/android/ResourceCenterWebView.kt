package com.commandbar.android

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.commandbar.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.lang.ref.WeakReference
import org.json.JSONObject


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
    private val engagementShell: String = "resource-center",
    private val engagementInitialPage: String = "help-hub",
) : WebView(context) {
    private lateinit var options: CommandBarOptions
    private var onFallbackAction: FallbackActionCallback = onFallbackAction ?: { }
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var articleId: Int?

    // Ensures the engagement boot snippet is injected at most once per WebView instance, so a
    // repeated onPageFinished cannot kick off a second engagement boot in the same document.
    private var hasInjectedBootSnippet = false

    init {
        this.articleId = articleId
        resetJavascriptInterface(this.onFallbackAction)

        if (options != null) {
            this.options = options
            setupWebView(options, this.articleId)
        }

        // Self-register as the live target for `CommandBar.setResourceCenterFilter` /
        // `setAssistantFilter`. Mirrors iOS `ResourceCenterWebView.activeInstance = self`.
        // Both modal-opened views (created in `CommandBar.openResourceCenter` /
        // `openAssistant`) and inline RN views (created via `ResourceCenterViewManager`)
        // flow through this constructor, so both surfaces receive live filter updates.
        // We use a `WeakReference` so the static does not pin the view past its natural
        // lifecycle; `getActiveInstance()` returns `null` once GC has reclaimed it.
        activeInstanceRef = WeakReference(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // If we're still the active instance when detached, clear the reference so
        // subsequent `applyEngagementFilters()` calls do not attempt to drive a view
        // that has been removed from the window tree.
        if (activeInstanceRef?.get() === this) {
            activeInstanceRef = null
        }
    }

    // When this WebView lives inside a Material BottomSheetDialog, the parent
    // BottomSheetBehavior intercepts vertical drags before they reach the WebView.
    // We can't rely on the WebView's native canScrollVertically() to decide who owns
    // the gesture: the engagement UI (e.g. the Assistant chat history) scrolls inside
    // an inner `overflow: scroll` web container, so the document itself reports that it
    // can't scroll and the sheet wrongly steals the drag. Instead, while a touch is in
    // progress on the WebView we claim the whole gesture so web content (including inner
    // scrollers) scrolls naturally. The sheet can still be dismissed via the drag handle
    // strip above the WebView, the dimmed backdrop, the system back button, or the
    // in-UI close control.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                requestParentDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                requestParentDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun requestParentDisallowInterceptTouchEvent(disallow: Boolean) {
        parent?.requestDisallowInterceptTouchEvent(disallow)
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
        fun engagement__onFallbackAction(action: String?) {
            handleFallbackAction(action)
        }

        private fun handleFallbackAction(action: String?) {
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
            removeJavascriptInterface("EngagementAndroidInterface")
        } catch (e: Exception) {
            // Do nothing
        } finally {
            addJavascriptInterface(CommandBarJavaScriptInterface(callback), "EngagementAndroidInterface")
        }
    }

    private fun setupWebView (options: CommandBarOptions, articleId: Int? = null) {
        // A bare WebChromeClient does nothing for `<input type="file">`. Override
        // onShowFileChooser so the Assistant's image/file upload control opens the system
        // file picker (matching iOS's WKWebView, which handles this natively).
        webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (filePathCallback == null) return false
                return EngagementFileChooser.show(context, fileChooserParams, filePathCallback)
            }

            // Article references in the Assistant open via `window.open()` / `target="_blank"`
            // (the same path iOS handles in WKUIDelegate.createWebViewWith). Without multi-window
            // support + this handler, Android loads the link into THIS WebView, replacing the
            // engagement document; onPageFinished then re-injects the boot snippet and a second
            // Assistant renders. We instead route the popup URL to the external browser and never
            // navigate the main WebView. The link target is captured via a throwaway WebView whose
            // first navigation we intercept (window.open URLs are not otherwise exposed here).
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                val host = view ?: return false
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false

                val hrefProbe = WebView(host.context)
                hrefProbe.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        probeView: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        request?.url?.let { openUrlExternally(host.context, it) }
                        probeView?.destroy()
                        return true
                    }
                }
                transport.webView = hrefProbe
                resultMsg.sendToTarget()
                return true
            }
        }
        setBackgroundColor(Color.TRANSPARENT)

        // Enable JavaScript in the WebView
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Route `window.open()` / `target="_blank"` (e.g. Assistant article references) through
        // onCreateWindow so they open externally instead of replacing the engagement document.
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true

        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )


        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val webView = view ?: return
                // Only (re)inject the boot snippet into our own engagement document. If a
                // navigation ever lands the main frame on some other URL, re-injecting would boot
                // a duplicate Assistant — so we skip anything that is not our base document.
                if (!isEngagementDocumentUrl(url)) return
                // Inject at most once per instance. The injected JS also guards itself, but this
                // native gate avoids even evaluating a second snippet for the same document.
                if (hasInjectedBootSnippet) return
                webView.evaluateJavascript(
                    "(function() { return !!window.__ampMobileResourceCenterLoaded; })();")
                    { result ->
                        if (!isEvaluateJavascriptTruthy(result) && !hasInjectedBootSnippet) {
                            hasInjectedBootSnippet = true
                            val snippet = getSnippet(options, articleId, engagementShell, engagementInitialPage)
                            webView.evaluateJavascript(snippet) { }
                        }
                    }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return false
                openUrlExternally(view?.context ?: context, url)
                return true
            }
        }

        val html = getHTML(options)
        loadDataWithBaseURL(ENGAGEMENT_BASE_URL, html, "text/html", "UTF-8", null)
    }

    /** True for the engagement SPA document we host (the only page we boot into). */
    private fun isEngagementDocumentUrl(url: String?): Boolean {
        if (url.isNullOrEmpty() || url == "about:blank") return true
        return url.startsWith(ENGAGEMENT_BASE_URL)
    }

    /** Opens a link in the device browser, swallowing the case where no handler exists. */
    private fun openUrlExternally(context: Context, url: Uri) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (e: Exception) {
            Log.w(TAG_RESOURCE_CENTER_WEB_VIEW, "No activity to open url: $url", e)
        }
    }

    fun openBottomSheetDialog() {
        // Guard against stacking a second sheet for this instance (e.g. a double open call).
        if (bottomSheetDialog != null) {
            bottomSheetDialog?.show()
            return
        }
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

    /** Tears down the in-WebView engagement UI (`assistant.close()` or hide Resource Center). */
    fun closeEngagementShell() {
        evaluateJavascript(buildCloseEngagementShellJavaScript()) { }
    }

    private fun isEvaluateJavascriptTruthy(result: String?): Boolean {
        val trimmed = result?.trim() ?: return false
        return trimmed == "true" || trimmed == "\"true\""
    }

    companion object {
        internal const val TAG_RESOURCE_CENTER_WEB_VIEW = "ResourceCenterWebView"

        /** Base URL used for `loadDataWithBaseURL`; also the document we boot the engagement SPA into. */
        private const val ENGAGEMENT_BASE_URL = "https://cdn.amplitude.com"

        /**
         * The most recently constructed `ResourceCenterWebView`, used by
         * `CommandBar.setResourceCenterFilter` / `setAssistantFilter` to push live
         * filter changes into the JS layer. Weak so the static reference does not
         * prevent GC of an old WebView that is no longer attached. Mirrors iOS
         * `ResourceCenterWebView.activeInstance` (which is a `weak var`).
         */
        @JvmStatic
        private var activeInstanceRef: WeakReference<ResourceCenterWebView>? = null

        /** Returns the currently active [ResourceCenterWebView], or `null` if none. */
        @JvmStatic
        fun getActiveInstance(): ResourceCenterWebView? = activeInstanceRef?.get()

        @JvmStatic
        fun buildCloseEngagementShellJavaScript(): String {
            return """
                (function() {
                    var shell = window.__ampEngagementShell || "resource-center";
                    if (shell === "assistant") {
                        try {
                            if (window.engagement && window.engagement.assistant && typeof window.engagement.assistant.close === "function") {
                                window.engagement.assistant.close();
                            }
                        } catch (e1) {}
                        return;
                    }
                    try {
                        if (window.engagement && typeof window.engagement._showResourceCenter === "function") {
                            window.engagement._showResourceCenter(false);
                        }
                    } catch (e2) {}
                })();
            """.trimIndent()
        }

        @JvmStatic
        fun buildApplyEngagementFiltersJavaScript(): String {
            val rc = EngagementFilterStore.resourceCenterFilterJsonLiteral()
            val assistant = EngagementFilterStore.assistantFilterJsonLiteral()
            // We always mutate the `window.__ampNativeRCFilter` / `__ampNativeAssistantFilter`
            // globals so the boot-time snippet picks up the latest value when it runs
            // `applyNativeEngagementFilters()` after `engagement.boot()` resolves. This
            // avoids a race where the native filter was updated between WebView
            // construction and engagement boot completion. If engagement is already
            // booted, we also push the change live. `null` (clear) propagates through
            // both paths intentionally.
            return """
                (function() {
                    window.__ampNativeRCFilter = $rc;
                    window.__ampNativeAssistantFilter = $assistant;
                    try {
                        if (window.engagement && typeof window.engagement.setResourceCenterFilter === "function") {
                            window.engagement.setResourceCenterFilter(window.__ampNativeRCFilter);
                        }
                    } catch (e1) {}
                    try {
                        if (window.engagement && window.engagement.assistant && typeof window.engagement.assistant.setAssistantFilter === "function") {
                            window.engagement.assistant.setAssistantFilter(window.__ampNativeAssistantFilter);
                        }
                    } catch (e2) {}
                })();
            """.trimIndent()
        }

        /**
         * Serializes a [CommandBarUser] to the `{ user_id, device_id }` JSON shape the web SDK expects,
         * or `"null"` when no identity was supplied.
         */
        private fun buildNativeUserJsLiteral(user: CommandBarUser?): String {
            if (user == null || (user.userId == null && user.deviceId == null)) return "null"
            val json = JSONObject()
            user.userId?.let { json.put("user_id", it) }
            user.deviceId?.let { json.put("device_id", it) }
            return json.toString()
        }

        @JvmStatic
        private fun getSnippet(
            options: CommandBarOptions,
            articleId: Int? = null,
            engagementShell: String = "resource-center",
            engagementInitialPage: String = "help-hub",
        ): String {
            val apiKey = JSONObject.quote(options.apiKey)
            val nativeUserJs = buildNativeUserJsLiteral(options.user)
            val articleIdJs = articleId?.toString() ?: "null"
            val engagementShellJs = JSONObject.quote(engagementShell)
            val engagementInitialPageJs = JSONObject.quote(engagementInitialPage)
            val serverZone = JSONObject.quote(options.serverZone.value)
            val serverUrlJs = options.serverUrl?.let { JSONObject.quote(it) } ?: "null"
            val cdnUrlJs = options.cdnUrl?.let { JSONObject.quote(it) } ?: "null"
            val chatUrlJs = options.chatUrl?.let { JSONObject.quote(it) } ?: "null"
            val mediaUrlJs = options.mediaUrl?.let { JSONObject.quote(it) } ?: "null"
            val localeJs = options.locale?.let { JSONObject.quote(it) } ?: "null"
            val resourceCenterFilterJs = EngagementFilterStore.resourceCenterFilterJsonLiteral()
            val assistantFilterJs = EngagementFilterStore.assistantFilterJsonLiteral()
            return """
              (function() {
                  window._ampIsWebView = true;

                  function ampLog(msg) {
                      try {
                          if (window.EngagementAndroidInterface && typeof window.EngagementAndroidInterface.engagement__log === "function") {
                              window.EngagementAndroidInterface.engagement__log(String(msg));
                          }
                      } catch (e) {}
                      try { console.log(msg); } catch (e2) {}
                  }

                  function notifyNativeResourceCenterClosed() {
                      try {
                          if (window.EngagementAndroidInterface && typeof window.EngagementAndroidInterface.onResourceCenterClose === "function") {
                              window.EngagementAndroidInterface.onResourceCenterClose();
                              return;
                          }
                      } catch (eDedicated) {}
                      var payload = JSON.stringify({ meta: { type: "close" } });
                      try {
                          var iface = window.EngagementAndroidInterface;
                          if (iface && typeof iface.engagement__onFallbackAction === "function") {
                              iface.engagement__onFallbackAction(payload);
                          }
                      } catch (eFallback) {}
                  }

                  function isResourceCenterCloseElement(el) {
                      if (!el || typeof el.getAttribute !== "function") { return false; }
                      var tag = (el.tagName || "").toUpperCase();
                      var role = (el.getAttribute("role") || "").toLowerCase();
                      var label = el.getAttribute("aria-label") || "";
                      if (!/^close$/i.test(label)) { return false; }
                      if (tag !== "BUTTON" && role !== "button") { return false; }
                      // The search input's clear button shares aria-label="close" but lives inside
                      // #resource-center-input-container. Only the header close should dismiss the widget.
                      if (typeof el.closest === "function" && el.closest("#resource-center-input-container")) { return false; }
                      return true;
                  }

                  function eventIndicatesResourceCenterClose(ev) {
                      var path = typeof ev.composedPath === "function" ? ev.composedPath() : [];
                      var i = 0;
                      for (i = 0; i < path.length; i++) {
                          if (isResourceCenterCloseElement(path[i])) { return true; }
                      }
                      var t = ev.target;
                      if (t && typeof t.closest === "function") {
                          var btn = t.closest('button[aria-label="close"], button[aria-label="Close"], [role="button"][aria-label="close"], [role="button"][aria-label="Close"]');
                          if (btn && isResourceCenterCloseElement(btn)) { return true; }
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
                          closeEngagementShell();
                          notifyNativeResourceCenterClosed();
                      }
                      document.addEventListener("pointerdown", relay, true);
                      document.addEventListener("click", relay, true);
                      document.addEventListener("touchend", relay, true);
                  }
                  installNativeResourceCenterCloseBridge();

                  // Synchronous duplicate-boot guard. window.__ampMobileResourceCenterLoaded is only
                  // set once boot() resolves and the shell is shown (async, several frames later), so
                  // it cannot guard against a second snippet injection that races in before then.
                  // __ampEngagementBootInvoked is set synchronously right here, so any later injection
                  // in the same document returns immediately and cannot start a second boot.
                  if (window.__ampMobileResourceCenterLoaded || window.__ampEngagementBootInvoked) { return; }
                  window.__ampEngagementBootInvoked = true;

                  var apiKey = $apiKey;
                  var serverZone = $serverZone;
                  var nativeUser = $nativeUserJs;
                  var nativeServerUrl = $serverUrlJs;
                  var nativeCdnUrl = $cdnUrlJs;
                  var nativeChatUrl = $chatUrlJs;
                  var nativeMediaUrl = $mediaUrlJs;
                  var nativeLocale = $localeJs;
                  var articleId = $articleIdJs;
                  var engagementShell = $engagementShellJs;
                  window.__ampEngagementShell = engagementShell;
                  var engagementInitialPage = $engagementInitialPageJs;
                  // Initialise the native-filter globals from the latest value the native
                  // side has cached at WebView construction time. `applyEngagementFilters()`
                  // (called via `evaluateJavascript` whenever native updates the filter) will
                  // overwrite these globals, so by the time `applyNativeEngagementFilters()`
                  // runs after `engagement.boot()` resolves, we always read the latest value.
                  window.__ampNativeRCFilter = $resourceCenterFilterJs;
                  window.__ampNativeAssistantFilter = $assistantFilterJs;

                  function applyNativeEngagementFilters() {
                      try {
                          if (window.engagement && typeof window.engagement.setResourceCenterFilter === "function") {
                              window.engagement.setResourceCenterFilter(window.__ampNativeRCFilter);
                          }
                      } catch (eRc) {}
                      try {
                          if (window.engagement && window.engagement.assistant && typeof window.engagement.assistant.setAssistantFilter === "function") {
                              window.engagement.assistant.setAssistantFilter(window.__ampNativeAssistantFilter);
                          }
                      } catch (eAsst) {}
                  }

                  /**
                   * The host page normally loads custom fonts (see base/public/index.html). Inside a WebView there is no host
                   * page, so any theme using a non-system family (e.g. "Inter", "IBM Plex Sans") falls back to system fonts
                   * unless we fetch the font ourselves.
                   */
                  var GENERIC_FONT_NAMES = {
                      "system-ui": 1, "sans-serif": 1, "serif": 1, "monospace": 1,
                      "cursive": 1, "fantasy": 1, "ui-serif": 1, "ui-sans-serif": 1,
                      "ui-monospace": 1, "ui-rounded": 1, "inherit": 1, "initial": 1, "unset": 1,
                      "-apple-system": 1, "blinkmacsystemfont": 1,
                      "segoe ui": 1, "roboto": 1, "helvetica": 1, "arial": 1, "helvetica neue": 1,
                      "apple color emoji": 1, "segoe ui emoji": 1, "segoe ui symbol": 1
                  };

                  function extractPrimaryFontFamily(value) {
                      if (!value) { return null; }
                      var families = String(value).split(",");
                      for (var i = 0; i < families.length; i++) {
                          var name = families[i].trim().replace(/^["']|["']${'$'}/g, "").trim();
                          if (name && !GENERIC_FONT_NAMES[name.toLowerCase()]) {
                              return name;
                          }
                      }
                      return null;
                  }

                  function ensureGoogleFontLoaded(name) {
                      if (!name) { return; }
                      if (!window.__ampLoadedThemeFonts) { window.__ampLoadedThemeFonts = {}; }
                      if (window.__ampLoadedThemeFonts[name]) { return; }
                      window.__ampLoadedThemeFonts[name] = true;
                      var encoded = encodeURIComponent(name).replace(/%20/g, "+");
                      var link = document.createElement("link");
                      link.rel = "stylesheet";
                      link.href = "https://fonts.googleapis.com/css2?family=" + encoded +
                          ":ital,wght@0,300;0,400;0,500;0,600;0,700;1,300;1,400;1,500;1,600;1,700&display=swap";
                      document.head.appendChild(link);
                  }

                  function detectAndLoadThemeFont() {
                      try {
                          var widget = document.querySelector('[class*="engagement-widget"]');
                          if (!widget) { return false; }
                          var value = getComputedStyle(widget).getPropertyValue("--font-font-family");
                          var primary = extractPrimaryFontFamily(value);
                          if (primary) { ensureGoogleFontLoaded(primary); }
                          return true;
                      } catch (e) {
                          return false;
                      }
                  }

                  /** Polls until widget mounts, then keeps checking so mid-session theme switches still load the new font. */
                  function startThemeFontWatcher() {
                      var attempts = 0;
                      var maxAttempts = 200;
                      function tick() {
                          attempts++;
                          detectAndLoadThemeFont();
                          if (attempts >= maxAttempts) { return; }
                          setTimeout(tick, attempts < 50 ? 200 : 1000);
                      }
                      tick();
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
                      var cdnBase = nativeCdnUrl
                          ? nativeCdnUrl
                          : (serverZone === "EU" ? "https://cdn.eu.amplitude.com" : "https://cdn.amplitude.com");
                      return cdnBase + "/script/" + encodeURIComponent(apiKey) + ".engagement.js";
                  }

                  function engagementInitOptions() {
                      var o = { serverZone: serverZone };
                      if (nativeServerUrl) { o.serverUrl = nativeServerUrl; }
                      if (nativeCdnUrl) { o.cdnUrl = nativeCdnUrl; }
                      if (nativeChatUrl) { o.chatUrl = nativeChatUrl; }
                      if (nativeMediaUrl) { o.mediaUrl = nativeMediaUrl; }
                      if (nativeLocale) { o.locale = nativeLocale; }
                      return o;
                  }

                  function buildUser() {
                      if (nativeUser && (nativeUser.user_id || nativeUser.device_id)) {
                          return nativeUser;
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

                  function closeEngagementShell() {
                      if (engagementShell === "assistant") {
                          try {
                              if (window.engagement && window.engagement.assistant && typeof window.engagement.assistant.close === "function") {
                                  window.engagement.assistant.close();
                              }
                          } catch (e1) {}
                          return;
                      }
                      try {
                          if (window.engagement && typeof window.engagement._showResourceCenter === "function") {
                              window.engagement._showResourceCenter(false);
                          }
                      } catch (e2) {}
                  }

                  function openAssistant() {
                      hideNativeLoadingSpinner();
                      try {
                          window.dispatchEvent(new Event("resize"));
                      } catch (e1) {}
                      requestAnimationFrame(function () {
                          requestAnimationFrame(function () {
                              try {
                                  if (window.engagement && window.engagement.assistant && typeof window.engagement.assistant.show === "function") {
                                      window.engagement.assistant.show({ initialPage: "chat" });
                                  }
                                  try {
                                      window.dispatchEvent(new Event("resize"));
                                  } catch (e2) {}
                              } catch (e3) {
                                  ampLog("[Amplitude Engagement] assistant.show: " + e3);
                              }
                              window.__ampMobileResourceCenterLoaded = true;
                          });
                      });
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

                  function openEngagementShell() {
                      // Idempotent: even if boot resolves more than once, only show the shell once.
                      if (window.__ampEngagementShellShown) { return; }
                      window.__ampEngagementShellShown = true;
                      if (engagementShell === "assistant") {
                          openAssistant();
                      } else {
                          openResourceCenter();
                      }
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
                                      openEngagementShell();
                                      startThemeFontWatcher();
                                  }).catch(function (err) {
                                      ampLog("[Amplitude Engagement] boot failed: " + err);
                                  });
                              } else {
                                  applyNativeEngagementFilters();
                                  openEngagementShell();
                                  startThemeFontWatcher();
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

        /**
         * Builds `<link>` tags that preload the given Google Font families. The WebView has no host
         * page to load fonts, so a theme using a non-system family only renders if we fetch it here.
         * Returns an empty string when no families are supplied.
         */
        @JvmStatic
        private fun buildFontPreloadLinks(families: List<String>): String {
            return families
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(separator = "\n                  ") { family ->
                    // URLEncoder encodes spaces as "+", which is exactly what the Google Fonts API expects.
                    val encoded = java.net.URLEncoder.encode(family, "UTF-8")
                    "<link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css2?family=$encoded&display=swap\">"
                }
        }

        @JvmStatic()
        private  fun getHTML(options: CommandBarOptions): String {
            val fontLinks = buildFontPreloadLinks(options.fontFamilies)
            return """
            <!DOCTYPE html>
            <html>
              <head>
                  <meta name="viewport" content="user-scalable=no, width=device-width, height=device-height, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=no">
                  <link rel="preconnect" href="https://fonts.googleapis.com">
                  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                  $fontLinks
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

                      /* Hide the Resource Center "Copy link" header button in the mobile WebView only. */
                      [data-testid="resource-center-copy-link"] {
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
