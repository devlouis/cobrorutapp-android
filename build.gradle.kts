import com.android.build.api.dsl.LibraryExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

val sharedCompileSdk = libs.versions.compileSdk.get().toInt()
val sharedMinSdk = libs.versions.minSdk.get().toInt()
val sharedJavaTarget = JavaVersion.toVersion(libs.versions.javaTarget.get())

subprojects {
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileSdk = sharedCompileSdk
            defaultConfig.minSdk = sharedMinSdk
            compileOptions {
                sourceCompatibility = sharedJavaTarget
                targetCompatibility = sharedJavaTarget
            }
        }
    }
}
