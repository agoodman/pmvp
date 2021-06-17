object Dependencies {
    object Kotlin {
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        const val coroutinesJvm = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
        const val junit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
        const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    }
    object Ktor {
        const val core = "io.ktor:ktor-client-core:${Versions.ktor}"
    }
    object SqlDelight {
        const val core = "com.squareup.sqldelight:coroutines-extensions:${Versions.sqldelight}"
    }
}
