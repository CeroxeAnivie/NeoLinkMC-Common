plugins {
    `java-library`
    `maven-publish`
    signing
    jacoco
}

group = property("maven_group").toString()
version = property("mod_version").toString()

val neoLinkApiVersion = property("neolinkapi_version").toString()
val jetbrainsAnnotationsVersion = "26.0.2"
val junitVersion = "5.11.4"

repositories {
    if (providers.gradleProperty("useMavenLocal").map(String::toBoolean).orElse(false).get()) {
        mavenLocal()
    }
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8",
        "-Dconsole.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8"
    )
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencies {
    implementation("top.ceroxe.api:neolinkapi-desktop:$neoLinkApiVersion") {
        exclude(group = "org.slf4j", module = "slf4j-nop")
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.slf4j:slf4j-api:2.0.16")
    compileOnly("org.jetbrains:annotations:$jetbrainsAnnotationsVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "neolinkmc-common"

            pom {
                name.set("NeoLinkMC Common")
                description.set("Shared JVM core, config model and tunnel orchestration used by NeoLinkMC loader adapters.")
                url.set("https://github.com/CeroxeAnivie/NeoLinkMC-Common")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("CeroxeAnivie")
                        name.set("Ceroxe")
                        email.set("1591117599@qq.com")
                        organization.set("Ceroxe")
                        url.set("https://github.com/CeroxeAnivie")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/CeroxeAnivie/NeoLinkMC-Common.git")
                    developerConnection.set("scm:git:ssh://github.com:CeroxeAnivie/NeoLinkMC-Common.git")
                    url.set("https://github.com/CeroxeAnivie/NeoLinkMC-Common")
                }
            }
        }
    }

    repositories {
        maven {
            name = "centralStaging"
            url = layout.buildDirectory.dir("repos/central-staging").get().asFile.toURI()
        }
        maven {
            name = "localDevelopment"
            url = layout.buildDirectory.dir("repos/local-development").get().asFile.toURI()
        }
    }
}

signing {
    val signingRequested = gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("publish", ignoreCase = true) && !taskName.contains("MavenLocal", ignoreCase = true)
    }
    isRequired = signingRequested
    if (signingRequested) {
        useGpgCmd()
        sign(publishing.publications["mavenJava"])
    }
}
