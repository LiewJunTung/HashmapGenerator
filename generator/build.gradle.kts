plugins {
    kotlin("jvm")
    kotlin("kapt")
    `maven-publish`
}

val artifactId = "generator"
group = "org.liewjuntung"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":annotation"))
    implementation("com.squareup:kotlinpoet:1.5.0")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    implementation("net.ltgt.gradle.incap:incap:0.3")
    kapt("com.google.auto.service:auto-service:1.0-rc7")
    kapt("net.ltgt.gradle.incap:incap-processor:0.3")

}
