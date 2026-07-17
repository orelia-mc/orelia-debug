plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.1"
}

group = "rpg"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    // TEMP DEV LOOP: prefers locally-published orelia-core/orelia-world/orelia-extra (run
    // `./gradlew publishToMavenLocal` in those repos) over jitpack, so in-flight changes are
    // picked up without a push. Remove this line before merging - production builds should
    // resolve from jitpack only.
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // Resolves orelia-core/orelia-world/orelia-extra straight from their GitHub repos, same
    // as orelia-extra does for orelia-core/orelia-world.
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    // VaultAPI's POM pulls in an old org.bukkit:bukkit:1.13.1 as a transitive dependency,
    // which conflicts with the org.bukkit:bukkit capability paper-api provides - exclude it.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    // orelia-debug only ever calls into these through their published rpg.api/rpg.world.api/
    // rpg.extra.api interfaces (Bukkit ServicesManager) - never gameplay-module internals
    // directly. OreliaCore is a hard dependency (plugin.yml depend); OreliaWorld/OreliaExtra
    // are soft (plugin.yml softdepend) - every world/extra API lookup must null-guard.
    compileOnly("com.github.orelia-mc:orelia-core:main-SNAPSHOT")
    compileOnly("com.github.orelia-mc:orelia-world:main-SNAPSHOT")
    compileOnly("com.github.orelia-mc:orelia-extra:main-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("orelia-debug")
    }
    build {
        dependsOn(shadowJar)
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    test {
        useJUnitPlatform()
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
