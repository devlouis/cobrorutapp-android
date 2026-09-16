plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pe.cobrorutapp.integration.notifications"
}

dependencies {
    implementation(project(":core:domain"))

    debugImplementation(libs.hilt.android)
    debugImplementation(libs.kotlinx.coroutines.android)
    add("kspDebug", libs.hilt.compiler)

    testDebugImplementation(libs.junit)
}
