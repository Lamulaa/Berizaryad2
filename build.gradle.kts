// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.12.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
}

// Примечание: блоки `allprojects` и `subprojects` в новых проектах Android
// обычно не нужны, так как управление репозиториями происходит через settings.gradle.kts