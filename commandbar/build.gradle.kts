import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("com.vanniktech.maven.publish") version "0.25.3"
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
    mavenPublishing {
        publishToMavenCentral(SonatypeHost.S01)

        signAllPublications()

        coordinates("com.commandbar.android", "commandbar", "1.0.9")

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
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}