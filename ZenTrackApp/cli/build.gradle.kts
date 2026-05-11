plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

group = "me.dcueto.zentrackapp"
version = "1.0.0"

application {
    mainClass.set("me.dcueto.zentrackapp.cli.MainKt")
    applicationName = "zentrack"
}

dependencies {
    implementation(projects.shared)
    implementation(libs.clikt)
    implementation(libs.koin.core)
    implementation(libs.jline.reader)
    implementation(libs.jline.terminal)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit)
}
kotlin {
    jvmToolchain(17)
}