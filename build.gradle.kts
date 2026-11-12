// Arquivo raiz intencionalmente enxuto: cada módulo declara seus próprios plugins
// via version catalog (gradle/libs.versions.toml). Assim o módulo :core continua
// compilando mesmo sem acesso ao Android SDK (veja settings.gradle.kts).

tasks.register("labInfo") {
    group = "help"
    description = "Mostra um resumo dos módulos e laboratórios do projeto."
    val names = subprojects.map { it.path }
    doLast {
        println("Android Java Lab")
        println("Módulos: ${names.joinToString(", ")}")
        println("Dica: ./gradlew -PcoreOnly test roda os engines sem Android SDK.")
    }
}
