plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "ru.it_spectrum.ai.playwright.mcp"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.ai.mcp.server)
    implementation(libs.playwright)

    testImplementation(libs.spring.boot.starter.test)
}

tasks.jar {
    enabled = false
}

tasks.bootJar {
    archiveBaseName.set("playwright-mcp-server")
    archiveVersion.set("")
    archiveClassifier.set("")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform {
        excludeTags("integration", "smoke")
    }
}

tasks.register<Test>("integrationTest") {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
        excludeTags("smoke")
    }
    group = "verification"
    description = "Runs Playwright integration tests against local test pages"
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("smokeTest") {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("smoke")
    }
    group = "verification"
    description = "Runs packaged MCP stdio smoke tests"
    shouldRunAfter(tasks.test)
    dependsOn(tasks.bootJar)
}

tasks.register<JavaExec>("installPlaywrightBrowsers") {
    group = "setup"
    description = "Installs Chromium browser binaries used by Playwright Java tests"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.microsoft.playwright.CLI")
    args("install", "chromium")
}
