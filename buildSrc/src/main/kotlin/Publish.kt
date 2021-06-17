import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

fun Project.applyMavenPublish(group: String, name: String, version: String) {

    apply(plugin = "maven-publish")

    this.group = group
    this.version = version

}
