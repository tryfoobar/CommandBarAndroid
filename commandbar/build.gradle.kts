plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    // Publishes to the Maven Central Portal (central.sonatype.com). 0.34.0 is the
    // latest version compatible with the pinned toolchain (Gradle 8.6 / AGP 8.2.1
    // / Kotlin 1.9.22); 0.35.0+ requires Gradle 8.13+ / AGP 8.2.2+. This version
    // already drops the sunset OSSRH host and defaults to the Central Portal.
    id("com.vanniktech.maven.publish") version "0.34.0"
}

android {
    namespace = "com.commandbar"
    compileSdk = 33

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

mavenPublishing {
    // No SonatypeHost argument: the plugin uploads to the new Central Portal
    // (central.sonatype.com). Authenticate with your Central Portal user token via
    // `mavenCentralUsername` / `mavenCentralPassword` (see README "Publishing").
    publishToMavenCentral()

    // GPG signing is required by Maven Central.
    signAllPublications()

    coordinates("com.commandbar.android", "commandbar", "2.0.0")

    pom {
        name.set("CommandBarAndroid")
        description.set("CommandBarSDK for Android")
        inceptionYear.set("2023")
        url.set("https://github.com/tryfoobar/CommandBarAndroid/")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/tryfoobar/CommandBarAndroid/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("tryfoobar")
                name.set("tryfoobar")
                url.set("https://github.com/tryfoobar/")
            }
        }
        scm {
            url.set("https://github.com/tryfoobar/CommandBarAndroid/")
            connection.set("scm:git:git://github.com/tryfoobar/CommandBarAndroid.git")
            developerConnection.set("scm:git:ssh://git@github.com/tryfoobar/CommandBarAndroid.git")
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}