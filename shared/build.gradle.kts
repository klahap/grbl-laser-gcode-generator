plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.io.core)
            implementation(libs.filekit.dialogs.compose)
            implementation(compose.materialIconsExtended)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.slf4j.api)
        }
        all {
            compilerOptions {
                freeCompilerArgs.addAll(
                    "-Xcontext-parameters",
                    "-Xexplicit-backing-fields",
                    "-Xexpect-actual-classes",
                )
            }
        }
    }
}

compose.desktop {
    application {
        nativeDistributions {
            windows {
                shortcut = true
                perUserInstall = true
            }
        }
    }
}
