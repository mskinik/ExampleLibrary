plugins {
    `java-platform`
    `maven-publish`
}

// A Bill of Materials (BoM) does not contain any code; it only pins the
// versions of the artifacts a consumer app should use. Apps depend on this
// platform once and drop version numbers from their own module dependencies.
javaPlatform {
    allowDependencies()
}

// Mirrors stringutils's tag-based versioning: prefer an explicit gradle
// property, otherwise derive the version from the pushed git tag (e.g.
// v1.0.0 -> 1.0.0) so the BoM always advertises the version that gets
// published in the same release.
val stringutilsVersion = (findProperty("stringutilsVersion") as String?)
    ?: System.getenv("GITHUB_REF_NAME")?.removePrefix("v")
    ?: "2.0.0"
val myModelLibraryVersion = "v2.0.0"
val bomVersion = (findProperty("bomVersion") as String?)
    ?: System.getenv("GITHUB_REF_NAME")?.removePrefix("v")
    ?: "1.0.0"

dependencies {
    constraints {
        api("com.github.mskinik:stringutils:$stringutilsVersion")
        api("com.github.mskinik:MyModelLibrary:$myModelLibraryVersion")
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["javaPlatform"])
            groupId = "com.github.mskinik"
            artifactId = "examplelibrary-bom"
            version = bomVersion
            pom {
                name.set("ExampleLibrary BoM")
                description.set("Bill of Materials pinning compatible versions of ExampleLibrary modules and their dependencies")
                url.set("https://github.com/mskinik/ExampleLibrary")
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mskinik/ExampleLibrary")
            credentials {
                username = System.getenv("GH_PACKAGES_USER") ?: ""
                password = System.getenv("GH_PACKAGES_TOKEN") ?: ""
            }
        }
    }
}

// See stringutils/build.gradle.kts for why: tools with incomplete Android/Gradle
// variant-aware resolution support (e.g. OpenRewrite's Gradle plugin) fail with
// AmbiguousVariantsFailure against multi-variant Gradle Module Metadata. The
// BoM's constraints are also encoded in the plain POM's <dependencyManagement>,
// so disabling module metadata here doesn't lose any functionality for
// standard consumers.
tasks.withType<org.gradle.api.publish.tasks.GenerateModuleMetadata> {
    enabled = false
}
