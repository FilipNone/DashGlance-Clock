plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 compiles Kotlin on its own. Declaring the Kotlin plugin here only sets which
    // compiler version AGP uses.
    alias(libs.plugins.kotlin.android) apply false
}
