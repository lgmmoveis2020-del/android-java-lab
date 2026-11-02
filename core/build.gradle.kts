plugins {
    `java-library`
}

// Sem toolchain fixa: compila com qualquer JDK >= 17 que esteja rodando o Gradle,
// mas sempre gera bytecode Java 17 (mesmo nível usado pelo módulo :app).
java {
    withSourcesJar()
}

dependencies {
    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(libs.versions.java.get().toInt())
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
