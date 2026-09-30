plugins {
    id("org.springframework.boot")
}

base {
    archivesName.set("sh-platform-scraper")
}

tasks.jar {
    enabled = false
}

val copyFrontendDist by tasks.registering(Copy::class) {
    from("../frontend/dist")
    into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.processResources {
    dependsOn(copyFrontendDist)
}

group = "com.scraper"

dependencies {
    implementation(project(":common"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("cn.idev.excel:fastexcel:1.2.0")
    
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // JPA 슬라이스 테스트용 인메모리 DB (설계 036 — searchRecent JPQL 파싱/동작 게이트)
    testImplementation("com.h2database:h2")
}
