applyMultiplatform()
applyMavenPublish(group = "org.pmvp", name = name, version = "0.0.2")

commonDependencies {
    implementation(project(":kmp:pmvp:pmvp-library"))
}

jvmDependencies {
    api("androidx.fragment:fragment-ktx:1.2.5")
}

// version notes
// 0.0.2 - updates after pmvp-library refactor.
// 0.0.1 - initial draft
