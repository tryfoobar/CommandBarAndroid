# Migrating from CommandBarAndroid 1.x to 2.0

CommandBarAndroid 2.0 is the **Amplitude Engagement** rewrite of the SDK. The renderer is now a single WebView that loads `*.engagement.js`, configuration is set once via a new `CommandBar.boot(...)` call, and the option names are aligned with the web Engagement SDK.

This is a **breaking** release. Every app on 1.x needs the changes below.

## TL;DR

| Area | 1.x | 2.0 |
| --- | --- | --- |
| Identifier | `orgId` (a CommandBar org id) | `apiKey` (your Amplitude project API key) |
| Configuration | passed to every `openResourceCenter` / `openAssistant` | passed once to `CommandBar.boot(...)` |
| Open methods | `openResourceCenter(activity, options, articleId?, onFallbackAction?)` | `openResourceCenter(activity, articleId?, onFallbackAction?)` |
| Open methods | `openAssistant(activity, options, onFallbackAction?)` | `openAssistant(activity, onFallbackAction?)` |
| `launchCode` | shortcut for staging/local endpoints | removed — use explicit `serverUrl` / `cdnUrl` / `chatUrl` / `mediaUrl` / `locale` / `serverZone` |
| User shape | `userId` only | `userId` (flat) **or** `user = CommandBarUser(userId, deviceId)` (nested) |

## 1. Boot once, instead of passing options on every call

The biggest API change: `CommandBar.boot(options)` stores the configuration. Subsequent `openResourceCenter` / `openAssistant` calls reuse it and take no options.

```diff
- val options = CommandBarOptions(orgId = "YOUR_ORG_ID", userId = "user-123")
-
- CommandBar.openResourceCenter(activity, options) {
-     CommandBar.closeResourceCenter()
- }
- CommandBar.openAssistant(activity, options)
+ // Once, e.g. in Application.onCreate or your launcher Activity:
+ CommandBar.boot(CommandBarOptions(apiKey = "YOUR_API_KEY", userId = "user-123"))
+
+ // Anywhere later — no options arg:
+ CommandBar.openResourceCenter(activity) {
+     CommandBar.closeResourceCenter()
+ }
+ CommandBar.openAssistant(activity)
```

Calls to `openResourceCenter` / `openAssistant` before `boot` are a no-op and log a warning. Call `boot` as early as possible.

## 2. Replace `orgId` with an Amplitude `apiKey`

CommandBar is now Amplitude Guides & Surveys. Get your project **API key** from the Amplitude dashboard and use it wherever you previously passed an org id.

```diff
- CommandBarOptions(orgId = "YOUR_ORG_ID")
+ CommandBarOptions(apiKey = "YOUR_API_KEY")
```

## 3. (Optional) Use the nested `user` form

For an explicit device id, use the new `CommandBarUser` nested form. The flat `userId =` shorthand still works.

```kotlin
// Flat (unchanged ergonomics)
CommandBar.boot(CommandBarOptions(apiKey = "YOUR_API_KEY", userId = "user-123"))

// Nested
CommandBar.boot(CommandBarOptions(
    apiKey = "YOUR_API_KEY",
    user = CommandBarUser(userId = "user-123", deviceId = "device-abc"),
))
```

## 4. Replace `launchCode` with explicit URLs / `serverZone`

`launchCode` is gone. To target staging, EU, or a local server, set the corresponding fields directly — these map 1:1 to the web Engagement SDK's `SDKConfig`.

```diff
- CommandBarOptions(orgId = "YOUR_ORG_ID", launchCode = "staging")
+ CommandBarOptions(
+     apiKey = "YOUR_API_KEY",
+     serverZone = ServerZone.EU,         // US (default), EU, LOCAL
+     serverUrl = "https://...",          // optional explicit override
+     cdnUrl    = "https://...",
+     chatUrl   = "https://...",
+     mediaUrl  = "https://...",
+     locale    = "en-US",
+ )
```

## 5. (Optional) Re-call `boot` after sign-in

`CommandBar.boot(...)` is safe to call again at any time. A common pattern is to boot anonymously at app launch and re-boot once the user authenticates:

```kotlin
CommandBar.boot(CommandBarOptions(apiKey = apiKey))
// ...later, after sign-in...
CommandBar.boot(CommandBarOptions(apiKey = apiKey, userId = signedInUserId))
```

## Reference

After migrating, the [README](./README.md) is the source of truth for the 2.0 API.
