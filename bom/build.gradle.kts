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

dependencies {
    constraints {
        api("com.github.mskinik:stringutils:${project.property("stringutilsVersion")}")
        api("com.github.mskinik:MyModelLibrary:${project.property("myModelLibraryVersion")}")
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["javaPlatform"])
            groupId = "com.github.mskinik"
            artifactId = "examplelibrary-bom"
            version = project.property("bomVersion") as String
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
