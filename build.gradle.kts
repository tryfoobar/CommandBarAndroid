// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath("io.github.gradle-nexus:publish-plugin:1.1.0")
    }
}

plugins {
    // AGP pinned to 8.2.1 to match React Native 0.74's gradle-plugin so that
    // CommandBarAndroid can be consumed via `includeBuild(...)` from the
    // react-native-commandbar example. Standalone Android builds work too.
    id("com.android.application") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.android.library") version "8.2.1" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0" apply true
}
