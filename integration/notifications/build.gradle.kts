plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pe.cobrorutapp.integration.notifications"
}

dependencies {
    implementation(project(":core:domain"))
}
