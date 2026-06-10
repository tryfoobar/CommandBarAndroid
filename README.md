<img src="docs/img/CommandBar.png" alt="CommandBar Logo" width="200" height="200"> <img src="https://www.freelogovectors.net/wp-content/uploads/2023/11/amplitude_logo-freelogovectors.net_.png" alt="Amplitude Logo" width="260" height="200">

# CommandBarAndroid

[![Build](https://github.com/tryfoobar/CommandBarAndroid/actions/workflows/ci.yml/badge.svg)](https://github.com/tryfoobar/CommandBarAndroid/actions/workflows/ci.yml)

Assistant & Resource Center in Android

> [!WARNING]
> CommandBar is now part of [Amplitude](https://amplitude.com). This repository has been updated to help existing CommandBar customers migrate to **Amplitude Resource Center & Assistant**, but it should be treated as **deprecated** and will not receive updates.
> For those migrating from CommandBar/CommandAI, please see our [migration guide](./MIGRATING.md)

## Requirements

-   Java 17
-   Android Studio

## Amplitude Guides & Surveys

The Resource Center / Assistant `WebView` loads the standalone Guides & Surveys script (`script/<API_KEY>.engagement.js`), then `init` + `boot` per the [Guides & Surveys SDK](https://amplitude.com/docs/sdks/guides-and-surveys/sdk).

- Pass your Amplitude **project API key** as `apiKey` in `CommandBarOptions`.
- Optional `serverZone`: `ServerZone.US` (default), `ServerZone.EU`, or `ServerZone.LOCAL`.
- Optional `serverUrl`, `cdnUrl`, `chatUrl`, `mediaUrl`, `locale` to override Amplitude endpoints (forwarded to `engagement.init`).

## Installation

### Maven Central

To add the CommandBarAndroid library to your project, you can use Maven Central. Add the following lines to your `build.gradle` file:

```groovy
repositories {
  mavenCentral()
}

dependencies {
    implementation 'com.commandbar.android:commandbar:1.0.10'
}
```

## Usage

```
import com.commandbar.android.CommandBar;
import com.commandbar.android.ResourceCenterWebView;
```

### `CommandBar`

`boot`: Stores your [`CommandBarOptions`] for reuse by every subsequent `openResourceCenter` / `openAssistant` call. Call once at app start (e.g. in `Application.onCreate`) and again to swap in new options after the user signs in.

```kotlin
CommandBar.boot(CommandBarOptions(apiKey = "YOUR_API_KEY"))
```

`openResourceCenter`: Opens Guides & Surveys Resource Center (Help Hub tab) in a BottomSheetDialog

`openAssistant`: Opens the Assistant tab in a BottomSheetDialog

`closeResourceCenter` / `closeAssistant`: Dismisses whichever engagement sheet
(Resource Center or Assistant) is currently presented. The two methods are
aliases — provided for API symmetry with `openResourceCenter` / `openAssistant`
— and are both no-ops when nothing is presented.

```kotlin
CommandBar.closeResourceCenter()
CommandBar.closeAssistant()
```

`setAssistantFilter` / `setResourceCenterFilter`: Mirrors `window.engagement.assistant.setAssistantFilter` and `window.engagement.setResourceCenterFilter`. Pass `null` to clear. Latest values apply on each WebView load and immediately when the sheet is open.

```kotlin
CommandBar.setAssistantFilter(JSONObject().put("tags", JSONArray().put("[Zendesk] mobile")))
CommandBar.setResourceCenterFilter(
    JSONObject().put(
        "and",
        JSONArray()
            .put(JSONObject().put("tags", JSONArray().put("[Zendesk] mobile")))
            .put(
                JSONObject().put(
                    "or",
                    JSONArray()
                        .put(JSONObject().put("tags", JSONArray().put("[Zendesk] v2")))
                        .put(JSONObject().put("tags", JSONArray().put("[Zendesk] v3")))
                )
            )
    )
)
```

`CommandBar.openResourceCenter(context, articleId?, onFallbackAction?)` / `CommandBar.openAssistant(context, onFallbackAction?)` arguments:

-   `context` (required): An instance of the Context/Activity to open a BottomSheetDialog on
-   `articleId` (optional, `openResourceCenter` only): Specify an article ID to deep-link into a specific article
-   `onFallbackAction` (optional): Callback fired when a Fallback CTA is interacted with in Assistant/Resource Center

Options for `CommandBar.boot(CommandBarOptions(...))`:

-   `apiKey` (required): Your Amplitude **project API key** for Guides & Surveys
-   `user` (optional): `CommandBarUser(userId = ..., deviceId = ...)`. Flat shorthand also accepted as `userId = ...` on the constructor.
-   `serverZone` (optional): `ServerZone.US` (default), `ServerZone.EU`, or `ServerZone.LOCAL`
-   `serverUrl`, `cdnUrl`, `chatUrl`, `mediaUrl`, `locale` (optional): forwarded to `engagement.init`
-   `spinnerColor` (optional): Color used by the loading spinner

```kotlin
// Flat shorthand
CommandBar.boot(CommandBarOptions(apiKey = "YOUR_API_KEY", userId = "user-123"))

// Nested form with explicit device id and overrides
CommandBar.boot(
    CommandBarOptions(
        apiKey = "YOUR_API_KEY",
        user = CommandBarUser(userId = "user-123", deviceId = "device-abc"),
        serverZone = ServerZone.EU,
    )
)
```

### `ResourceCenterWebView`

`init`: Loads ResourceCenter in a WebView. The WebView won't load its content until options are set on initialization or via setters.

-   `context` (required): An instance of the Context/Activity to attach to
-   `options` (optional): An instance of the `CommandBarOptions` class as documented above
-   `articleId` (optional): Optionally specify an article ID to open a specific article in ResourceCenter
-   `onFallbackAction` (optional): A callback function to receive an event when a Fallback CTA is interacted with in Assistant/Resource Center

### Run the Example

To run the example project, clone the repo and open the project in Android Studio.

## License

CommandBarAndroid is available under the MIT license. See the LICENSE file for more info.
