plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pe.cobrorutapp.core.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
}
