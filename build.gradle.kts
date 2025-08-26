plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.physicsgeek75"
version = "1.2.7"

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
      <![CDATA[
        <ul>
          <li>Fixed an issue where the cat appeared smaller than intended during idle pose.</li>
          <li>Fixed an issue where the cat would drift downwards during very fast typing.</li>
        </ul>
      ]]>
        """.trimIndent()
    }

    pluginVerification {
        ides {
            ide("IC", "2024.3")
            ide("IC", "2025.2")
            ide("PY", "2024.3")
            ide("PY","2025.2")
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
