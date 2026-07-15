plugins {
    id("org.springframework.boot")
}

group = "com.scraper"

dependencies {
    implementation(project(":sh-platform-common"))
    
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
    
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
