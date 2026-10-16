plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("me.clip:placeholderapi:2.12.3")

    implementation("io.github.revxrsal:zapper.api:1.0.3")
    implementation("dev.dejvokep:boosted-yaml:1.3.1")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.17.0")
    implementation("com.zaxxer:HikariCP:4.0.3")
    implementation("redis.clients:jedis:4.4.6")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-nop:1.7.36")
    implementation("org.bstats:bstats-bukkit:3.1.0")

    compileOnly("org.xerial:sqlite-jdbc:3.42.0.0")
    compileOnly("com.mysql:mysql-connector-j:8.4.0")
    compileOnly("com.h2database:h2:2.2.224")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

val relocateBase = "com.aquaticstudios.aquabook.libs"

tasks {
    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveBaseName = "AquaBook"
        archiveClassifier = ""

        relocate("revxrsal.zapper", "$relocateBase.zapper")
        relocate("dev.dejvokep.boostedyaml", "$relocateBase.boostedyaml")
        relocate("net.kyori", "$relocateBase.kyori")
        relocate("com.zaxxer.hikari", "$relocateBase.hikari")
        relocate("redis.clients.jedis", "$relocateBase.jedis")
        relocate("org.apache.commons.pool2", "$relocateBase.pool2")
        relocate("org.slf4j", "$relocateBase.slf4j")
        relocate("org.bstats", "$relocateBase.bstats")
        relocate("com.google.gson", "$relocateBase.gson")
        relocate("org.json", "$relocateBase.json")
        relocate("org.jetbrains.annotations", "$relocateBase.annotations")
        relocate("org.intellij.lang.annotations", "$relocateBase.intellij")

        exclude("META-INF/maven/**")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("module-info.class")
    }

    jar {
        archiveClassifier = "plain"
    }
}
