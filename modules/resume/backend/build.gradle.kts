plugins {
    id("org.springframework.boot")
}

base {
    archivesName.set("sh-platform-resume")
}

val copyFrontendDist by tasks.registering(Copy::class) {
    from("../frontend/dist")
    into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.processResources {
    dependsOn(copyFrontendDist)
}

tasks.jar {
    enabled = false
}

group = "com.shplatform"

dependencies {
    implementation(project(":common"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
