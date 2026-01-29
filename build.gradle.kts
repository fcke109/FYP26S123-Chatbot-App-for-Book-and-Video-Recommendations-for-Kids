// Top-level build file
plugins {
    id("com.android.application") version "9.0.0" apply false
    id("com.android.library") version "9.0.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.dagger.hilt.android") version "2.56.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

task("clean", Delete::class) {
    delete(layout.buildDirectory)
}
