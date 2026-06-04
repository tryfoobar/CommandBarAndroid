package com.commandbar.android

import org.json.JSONObject

/** Latest tag filters set from native; embedded into the WebView snippet and applied via JS after boot. */
object EngagementFilterStore {
    @Volatile
    var assistantFilterJson: String? = null
        private set

    @Volatile
    var resourceCenterFilterJson: String? = null
        private set

    fun assistantFilterJsonLiteral(): String = assistantFilterJson ?: "null"

    fun resourceCenterFilterJsonLiteral(): String = resourceCenterFilterJson ?: "null"

    fun setAssistantFilter(filter: JSONObject?) {
        assistantFilterJson = filter?.toString()
    }

    fun setResourceCenterFilter(filter: JSONObject?) {
        resourceCenterFilterJson = filter?.toString()
    }
}
