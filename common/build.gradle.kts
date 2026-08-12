plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-mail")

    // Web Push (VAPID)
    implementation("nl.martijndwars:web-push:5.1.2") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.bitbucket.b_c:jose4j:0.9.6")

    // JWT 검증 (RS256) — auth/scraper/resume/portfolio 공용 (nimbus-jose-jwt 포함)
    implementation("org.springframework.security:spring-security-oauth2-jose")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // File viewer module
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
}
