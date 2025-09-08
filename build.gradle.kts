// Berizaryad/build.gradle.kts
buildscript {
    dependencies {
        // Убедитесь, что версия AGP (Android Gradle Plugin) совместима с вашей версией Android Studio
        // и версией compileSdk в app/build.gradle.kts
        classpath("com.android.tools.build:gradle:8.2.0") // Или ваша версия
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0") // Или ваша версия Kotlin
        // Добавьте плагин Firebase, если используете Firebase Crashlytics или Performance Monitoring
        // classpath("com.google.gms:google-services:4.3.15") // Google Services plugin
        // classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Используем alias, если они определены в settings.gradle.kts
    // alias(libs.plugins.com.android.application) apply false
    // alias(libs.plugins.org.jetbrains.kotlin.android) apply false
    // Иначе, указываем напрямую:
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false // Если используется
}

// task clean(type: Delete) {
//     delete rootProject.buildDir
// }