pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Respaldo oficial de Vosk por si Maven Central tardara en publicar alguna versión.
        maven(url = "https://alphacephei.com/maven/")
    }
}
rootProject.name = "BEE-J"
include(":app")
