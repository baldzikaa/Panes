plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

dependencies {
    compileOnly(libs.paper.api)
    implementation(project(":panes"))
}

tasks {
    shadowJar {
        archiveClassifier = ""
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    runServer {
        minecraftVersion("26.1.2")
    }
}

runPaper.folia.registerTask()
