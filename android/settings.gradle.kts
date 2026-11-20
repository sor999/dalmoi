pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "dalmoi-android"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository { maven("https://repository.map.naver.com/archive/maven") }
            filter { includeGroup("com.naver.maps") }
        }
        google()
        mavenCentral()
    }
}

include(":app")
