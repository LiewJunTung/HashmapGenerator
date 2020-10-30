plugins {
    kotlin("jvm")
    kotlin("kapt")
}

val artifactId = "annotation"
group = "org.liewjuntung"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":annotation"))
    kapt(project(":generator"))
}
