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
    }
}

rootProject.name = "Pawchan"

include(":app")
include(":domain")
include(":data:network")
include(":data:database")
include(":data:preferences")
include(":update")

include(":data:drafts")
include(":data:maintenance")

include(":video-ffmpeg")

include(":core:ui")

include(":core:media")

include(":feature:composer")

include(":feature:thread")
