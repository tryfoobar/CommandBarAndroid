<img src="docs/img/CommandBar.png" alt="CommandBar Logo" width="200" height="200">

# CommandBarAndroid

[![Build](https://github.com/tryfoobar/CommandBarAndroid/actions/workflows/ci.yml/badge.svg)](https://github.com/tryfoobar/CommandBarAndroid/actions/workflows/ci.yml)

Assistant & Resource Center in Android

## Requirements

-   Java 17
-   Android Studio

## Amplitude Guides & Surveys

The Resource Center / Assistant `WebView` loads the standalone Guides & Surveys script (`script/<API_KEY>.engagement.js`), then `init` + `boot` per the [Guides & Surveys SDK](https://amplitude.com/docs/sdks/guides-and-surveys/sdk).

- Pass your Amplitude **project API key** as `orgId` in `CommandBarOptions`.
- Optional `serverZone`: `"US"` (default) or `"EU"`.

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

`openResourceCenter`: Opens Guides & Surveys Resource Center (Help Hub tab) in a BottomSheetDialog

`openAssistant`: Opens the Assistant tab in a BottomSheetDialog

`closeResourceCenter`: Dismisses the bottom sheet

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

-   `context` (required): An instance of the Context/Activity to open a BottomSheetDialog on
-   `options` (required): An instance of the `CommandBarOptions` class that holds the options for the `ResourceCenterWebView``.
    -   `orgId` (required): Your Amplitude **project API key** for Guides & Surveys (legacy CommandBar org id no longer applies to the Help Hub WebView)
    -   `spinnerColor` (optional): Optionally specify a color to render the loading Spinner
    -   `serverZone` (optional): `"US"` (default) or `"EU"`
-   `articleId` (optional): Optionally specify an article ID to open a specific article in ResourceCenter
-   `onFallbackAction` (optional): A callback function to receive an event when a Fallback CTA is interacted with in Assistant/Resource Center

### `ResourceCenterWebView`

`init`: Loads ResourceCenter in a WebView. The WebView won't load its content until options are set on intialization or via setters

-   `context` (required): An instance of the Context/Activity to attach to
-   `options` (optional): An instance of the `CommandBarOptions` class that holds the options for the `ResourceCenterWebView``.
    -   `orgId` (required): Your Amplitude **project API key** for Guides & Surveys (legacy CommandBar org id no longer applies to the Help Hub WebView)
    -   `spinnerColor` (optional): Optionally specify a color to render the loading Spinner
    -   `serverZone` (optional): `"US"` (default) or `"EU"`
-   `articleId` (optional): Optionally specify an article ID to open a specific article in ResourceCenter
-   `onFallbackAction` (optional): A callback function to receive an event when a Fallback CTA is interacted with in Assistant/Resource Center

### Run the Example

To run the example project, clone the repo and open the project in Android Studio.

## License

CommandBarAndroid is available under the MIT license. See the LICENSE file for more info.
