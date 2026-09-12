plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pe.cobrorutapp.integration.excel"
}

dependencies {
    implementation(project(":core:domain"))
}
