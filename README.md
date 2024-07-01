<img src="docs/img/CommandBar.png" alt="CommandBar Logo" width="200" height="200">

# CommandBarAndroid

[![Build](https://github.com/tryfoobar/CommandBarAndroid/actions/workflows/ci.yml/badge.svg)](https://github.com/tryfoobar/CommandBarAndroid/actions/workflows/ci.yml)

Copilot & HelpHub in Android

## Requirements

-   Java 17
-   Android Studio

## Installation

### Maven Central

To add the CommandBarAndroid library to your project, you can use Maven Central. Add the following lines to your `build.gradle` file:

```groovy
repositories {
  mavenCentral()
}

dependencies {
    implementation 'com.commandbar.android:commandbar:1.0.9'
}
```

## Usage

```
import com.commandbar.android.CommandBar;
import com.commandbar.android.HelpHubWebView;
```

### `CommandBar`

`openHelpHub`: Opens the HelpHub in a BottomSheetDialog

-   `context` (required): An instance of the Context/Activity to open a BottomSheetDialog on
-   `options` (required): An instance of the `CommandBarOptions` class that holds the options for the `HelpHubWebView``.
    -   `orgId` (required): Your Organization ID from [CommandBar](https://app.commandbar.com)
    -   `spinnerColor` (optional): Optionally specify a color to render the loading Spinner
-   `articleId` (optional): Optionally specify an article ID to open a specific article in HelpHub
-   `onFallbackAction` (optional): A callback function to receive an event when a Fallback CTA is interacted with

### `HelpHubWebView`

`init`: Loads HelpHub in a WebView. The WebView won't load its content until options are set on intialization or via setters

-   `context` (required): An instance of the Context/Activity to attach to
-   `options` (optional): An instance of the `CommandBarOptions` class that holds the options for the `HelpHubWebView``.
    -   `orgId` (required): Your Organization ID from [CommandBar](https://app.commandbar.com)
    -   `spinnerColor` (optional): Optionally specify a color to render the loading Spinner
-   `articleId` (optional): Optionally specify an article ID to open a specific article in HelpHub
-   `onFallbackAction` (optional): A callback function to receive an event when a Fallback CTA is interacted with

### Run the Example

To run the example project, clone the repo and open the project in Android Studio.

## License

CommandBarAndroid is available under the MIT license. See the LICENSE file for more info.
