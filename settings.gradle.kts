pluginManagement {
    repositories {
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://repo.maven.apache.org/maven2") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://repo.maven.apache.org/maven2") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "My Application"
include(":app")
