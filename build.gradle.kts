plugins {
    java
    id("io.freefair.maven-plugin") version "3.1.0"
    `maven-publish`
    signing
}

group = "com.ripstech.maven"
version = "1.0.0"

dependencies {
    implementation("org.apache.maven.plugin-tools:maven-plugin-annotations:3.5.2")
    implementation("org.apache.maven:maven-plugin-api:3.5.0")
    implementation("org.apache.maven:maven-core:3.3.3")
    implementation("com.ripstech.api:connector:3.1.1")
    implementation("com.ripstech.api:utils:3.1.1")
    implementation("org.jetbrains:annotations:16.0.3")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Sign>().configureEach {
    onlyIf {
        gradle.taskGraph.hasTask("publishToSonatype")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        create<MavenPublication>("rips-maven-plugin") {
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())
            pom {
                name.set("RIPS maven plugin")
                description.set("A plugin to start scans on a maven project.")
                url.set("https://www.ripstech.com")

                licenses {
                    license {
                        name.set("BSD-3-Clause")
                        url.set("https://github.com/rips/maven-plugin/blob/master/LICENSE")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        name.set("Amin Dada")
                        email.set("adada@ripstech.com")
                        organization.set("RIPS Technologies GmbH")
                        organizationUrl.set("https://ripstech.com")
                    }
                    developer {
                        name.set("Malena Ebert")
                        email.set("mebert@ripstech.com")
                        organization.set("RIPS Technologies GmbH")
                        organizationUrl.set("https://ripstech.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/rips/maven-plugin.git")
                    developerConnection.set("scm:git:ssh://github.com:rips/maven-plugin.git")
                    url.set("https://github.com/rips/maven-plugin/tree/master")
                }
            }
        }
    }
}