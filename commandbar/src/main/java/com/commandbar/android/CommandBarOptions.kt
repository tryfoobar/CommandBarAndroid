package com.commandbar.android

/**
 * Amplitude data residency / target endpoint family.
 * Maps to `SDKConfig.serverZone` in the Engagement web SDK.
 */
enum class ServerZone(val value: String) {
    US("US"),
    EU("EU"),
    LOCAL("local");

    companion object {
        @JvmStatic
        fun from(raw: String?): ServerZone {
            if (raw == null) return US
            return when (raw.uppercase()) {
                "EU" -> EU
                "LOCAL" -> LOCAL
                else -> US
            }
        }
    }
}

/** End user identity passed to `engagement.boot({ user, ... })`. */
data class CommandBarUser(
    var userId: String? = null,
    var deviceId: String? = null,
)

/**
 * Configuration for the Amplitude Engagement WebView SDK on Android.
 *
 * Native callers create one [CommandBarOptions] and the WebView routes fields to
 * `engagement.init(apiKey, {...})` and `engagement.boot({ user, ... })` internally,
 * matching the web `EngagementSDK` split.
 */
data class CommandBarOptions(
    /** Amplitude project API key. Routes to `engagement.init(apiKey, ...)`. */
    var apiKey: String,

    /**
     * End user identity. Routes to `engagement.boot({ user, ... })`.
     * If `null`, the WebView generates a session-scoped anonymous `device_id`.
     */
    var user: CommandBarUser? = null,

    /** Amplitude data residency. Routes to `engagement.init` `serverZone`. */
    var serverZone: ServerZone = ServerZone.US,

    /** Override Amplitude server endpoint. Routes to `engagement.init` `serverUrl`. */
    var serverUrl: String? = null,

    /**
     * Override the CDN base used for `*.engagement.js` and engagement-browser bundles.
     * Routes to `engagement.init` `cdnUrl` and is preferred for the bootstrap script URL.
     */
    var cdnUrl: String? = null,

    /** Override the Assistant chat endpoint. Routes to `engagement.init` `chatUrl`. */
    var chatUrl: String? = null,

    /** Override the media (image/video) endpoint. Routes to `engagement.init` `mediaUrl`. */
    var mediaUrl: String? = null,

    /** Localization locale (e.g. `"en-US"`). Routes to `engagement.init` `locale`. */
    var locale: String? = null,

    /** CSS color used by the loading spinner shown while the WebView boots Engagement. */
    var spinnerColor: String = "#3662F1",

    /**
     * Google Font families to preload in the WebView (e.g. `listOf("Roboto")`). The WebView has no
     * host page, so a theme that uses a non-system font only renders if that font is fetched here
     * (or auto-detected from the theme at runtime). List any custom theme fonts to guarantee they
     * load. Defaults to none.
     */
    var fontFamilies: List<String> = emptyList(),
) {
    /**
     * Flat-form convenience: pass `userId` directly without constructing a [CommandBarUser].
     *
     * `userId` is intentionally required (no default) so this overload only resolves
     * when the caller actually passes `userId =`. Without that, `CommandBarOptions(apiKey = "…")`
     * would be ambiguous with the primary constructor.
     */
    @JvmOverloads
    constructor(
        apiKey: String,
        userId: String?,
        serverZone: ServerZone = ServerZone.US,
        serverUrl: String? = null,
        cdnUrl: String? = null,
        chatUrl: String? = null,
        mediaUrl: String? = null,
        locale: String? = null,
        spinnerColor: String = "#3662F1",
        fontFamilies: List<String> = emptyList(),
    ) : this(
        apiKey = apiKey,
        user = userId?.let { CommandBarUser(userId = it) },
        serverZone = serverZone,
        serverUrl = serverUrl,
        cdnUrl = cdnUrl,
        chatUrl = chatUrl,
        mediaUrl = mediaUrl,
        locale = locale,
        spinnerColor = spinnerColor,
        fontFamilies = fontFamilies,
    )

    /**
     * Dictionary constructor used by the React Native bridge.
     * Accepts both the flat shorthand (`userId`) and nested (`user: { userId, deviceId }`) forms,
     * and falls back to the legacy `orgId` key for in-flight migrations.
     */
    constructor(dictionary: Map<String, Any?>) : this(
        apiKey = (dictionary["apiKey"] as? String)
            ?: (dictionary["orgId"] as? String)
            ?: "",
        user = parseUser(dictionary),
        serverZone = ServerZone.from(dictionary["serverZone"] as? String),
        serverUrl = dictionary["serverUrl"] as? String,
        cdnUrl = dictionary["cdnUrl"] as? String,
        chatUrl = dictionary["chatUrl"] as? String,
        mediaUrl = dictionary["mediaUrl"] as? String,
        locale = dictionary["locale"] as? String,
        spinnerColor = (dictionary["spinnerColor"] as? String) ?: "#3662F1",
        fontFamilies = parseFontFamilies(dictionary),
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun parseUser(dictionary: Map<String, Any?>): CommandBarUser? {
            val userDict = dictionary["user"] as? Map<String, Any?>
            if (userDict != null) {
                return CommandBarUser(
                    userId = userDict["userId"] as? String,
                    deviceId = userDict["deviceId"] as? String,
                )
            }
            val userId = dictionary["userId"] as? String
            return userId?.let { CommandBarUser(userId = it) }
        }

        /**
         * Reads the `fontFamilies` array (or singular `fontFamily` string) from the RN bridge
         * dictionary. Returns an empty list when neither is present.
         */
        private fun parseFontFamilies(dictionary: Map<String, Any?>): List<String> {
            (dictionary["fontFamilies"] as? List<*>)?.let { list ->
                return list.filterIsInstance<String>()
            }
            (dictionary["fontFamily"] as? String)?.let { return listOf(it) }
            return emptyList()
        }
    }
}
