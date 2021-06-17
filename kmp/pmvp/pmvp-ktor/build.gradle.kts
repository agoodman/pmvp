import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

applyMultiplatform()
applyMavenPublish(group = "org.pmvp", name = name, version = "0.0.2")

commonDependencies {
    implementation(project(":kmp:pmvp:pmvp-library"))
}

// version notes
// 0.0.2 - updates after pmvp-library refactor
// 0.0.1 - initial draft
