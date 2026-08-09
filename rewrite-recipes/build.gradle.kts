plugins {
    `java-library`
    `maven-publish`
}

// This module ships OpenRewrite recipes as plain data (YAML) under
// src/main/resources/META-INF/rewrite/*.yml. OpenRewrite's Gradle/Maven
// plugin auto-discovers any *.yml file on that classpath location, so no
// Java code or OpenRewrite dependencies are required just to author and
// publish declarative recipes.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// Mirrors the tag-based versioning already used by stringutils/bom: prefer
// an explicit gradle property, otherwise derive the version from the pushed
// git tag (e.g. v4.0.0 -> 4.0.0) so the recipe jar always advertises the
// same version as the library release it migrates consumers to.
val rewriteRecipesVersion = (findProperty("rewriteRecipesVersion") as String?)
    ?: System.getenv("GITHUB_REF_NAME")?.removePrefix("v")
    ?: "1.0.0"

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            groupId = "com.github.mskinik"
            artifactId = "examplelibrary-rewrite-recipes"
            version = rewriteRecipesVersion
            pom {
                name.set("ExampleLibrary OpenRewrite Recipes")
                description.set("OpenRewrite recipes automating source-code migrations for ExampleLibrary major version upgrades")
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
