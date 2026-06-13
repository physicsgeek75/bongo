import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.0"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "com.physicsgeek75"
version = "2.0.6"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2024.3")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

      // Add necessary plugin dependencies for compilation here, example:
      // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
        }

        changeNotes = """
        <ul>
          <li>NEW CUSTOMIZABLE DESIGNS FOR BONGOCAT:</li>
          <li>   Streamer Bongo</li>
          <li>   Minecraft Bongo</li>
          <li>   Nerd Bongo</li>
          <li>   Gangster Bongo</li>
          <li>   Royal Bongo</li>
        </ul>
        """.trimIndent()
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdea, "2024.3")
            create(IntelliJPlatformType.IntellijIdea, "2026.1")
            create(IntelliJPlatformType.PyCharmProfessional, "2024.3")
            create(IntelliJPlatformType.PyCharmProfessional, "2026.1")
        }
    }


}


java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
