/*
 * Copyright 2015 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.asciidoctor.gradle.jvm.AsciidoctorTask

plugins {

    // project plugins
    groovy

    kotlin("jvm") version "1.9.10"

    // test coverage
    jacoco

    // ide plugin
    idea

    // artifact signing - necessary on Maven Central
    signing

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    // documentation
    id("org.jetbrains.dokka") version "1.9.10"

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "1.2.1"
}

// release configuration
group = "com.intershop.gradle.scm"
description = "Gradle SCM version plugin - SCM based version handling for Gradle"
version = "6.2.1"

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

val pluginId = "com.intershop.gradle.scmversion"

gradlePlugin {
    plugins {
        website = "https://github.com/IntershopCommunicationsAG/${project.name}"
        vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
        create("scmversionPlugin") {
            id = pluginId
            implementationClass = "com.intershop.gradle.scm.ScmVersionPlugin"
            displayName = project.name
            description = project.description
            tags = listOf("intershop", "version", "release")
        }
    }
}


// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}

tasks {
    withType<Test>().configureEach {
        testLogging.showStandardStreams = false

        maxParallelForks = 1

        systemProperty("IDE_TEST_DEBUG_SUPPORT", "true")

        if (!System.getenv("GITUSER").isNullOrBlank() &&
                !System.getenv("GITPASSWD").isNullOrBlank() &&
                !System.getenv("GITURL").isNullOrBlank()) {
            systemProperty("giturl", System.getenv("GITURL"))
            systemProperty("gituser", System.getenv("GITUSER"))
            systemProperty("gitpasswd", System.getenv("GITPASSWD"))
        }

        if (!System.getProperty("GITUSER").isNullOrBlank() &&
                !System.getProperty("GITPASSWD").isNullOrBlank() &&
                !System.getProperty("GITURL").isNullOrBlank()) {
            systemProperty("giturl", System.getProperty("GITURL"))
            systemProperty("gituser", System.getProperty("GITUSER"))
            systemProperty("gitpasswd", System.getProperty("GITPASSWD"))
        }

        //Change directory for gradle tests
        systemProperty("org.gradle.native.dir", ".gradle")
        //Set supported Gradle version
        systemProperty("intershop.gradle.versions", "6.6,8.4")
        //working dir for tests
        systemProperty("intershop.test.base.dir", (File(project.layout.buildDirectory.get().asFile, "test-working")).absolutePath)
    }

    register<Copy>("copyAsciiDoc") {
        includeEmptyDirs = false

        val outputDir = file("${project.layout.buildDirectory.get().asFile}/tmp/asciidoctorSrc")
        val inputFiles = fileTree(mapOf("dir" to rootDir,
                "include" to listOf("**/*.asciidoc"),
                "exclude" to listOf("build/**")))

        inputs.files.plus(inputFiles)
        outputs.dir(outputDir)

        doFirst {
            outputDir.mkdir()
        }

        from(inputFiles)
        into(outputDir)
    }

    withType<AsciidoctorTask> {
        dependsOn("copyAsciiDoc")

        setSourceDir(file("${project.layout.buildDirectory.get().asFile}/tmp/asciidoctorSrc"))
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        outputOptions {
            setBackends(listOf("html5", "docbook"))
        }

        options = mapOf("doctype" to "article",
                "ruby" to "erubis")
        attributes = mapOf(
                "latestRevision" to project.version,
                "toc" to "left",
                "toclevels" to "2",
                "source-highlighter" to "coderay",
                "icons" to "font",
                "setanchors" to "true",
                "idprefix" to "asciidoc",
                "idseparator" to "-",
                "docinfo1" to "true")
    }

    withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)

            html.outputLocation.set(File(project.project.layout.buildDirectory.get().asFile, "jacocoHtml"))
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    getByName("jar").dependsOn("asciidoctor")

    dokkaJavadoc.configure {
        outputDirectory.set(project.layout.buildDirectory.get().asFile.resolve("dokka"))
    }

    task<Jar>("sourceJar") {
        description = "Creates a JAR that contains the source code."

        from(sourceSets.getByName("main").allSource)
        archiveClassifier.set("sources")
    }

    withType<Sign> {
        val sign = this
        withType<PublishToMavenLocal> {
            this.dependsOn(sign)
        }
        withType<PublishToMavenRepository> {
            this.dependsOn(sign)
        }
    }

    afterEvaluate {
        getByName<Jar>("javadocJar") {
            dependsOn(dokkaJavadoc)
            from(dokkaJavadoc)
        }
    }
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])

            artifact(File(project.layout.buildDirectory.get().asFile, "docs/asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(project.layout.buildDirectory.get().asFile, "docs/asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                organization {
                    name.set("Intershop Communications AG")
                    url.set("http://intershop.com")
                }
                developers {
                    developer {
                        id.set("m-raab")
                        name.set("M. Raab")
                        email.set("mraab@intershop.de")
                    }
                }
                scm {
                    connection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    developerConnection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
}

signing {
    sign(publishing.publications["intershopMvn"])
}

dependencies {
    implementation("com.intershop.gradle.version:extended-version:3.1.0")
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10")

    //jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:6.7.0.202309050840-r")


    testRuntimeOnly("org.apache.httpcomponents:httpclient:4.5.14")
    testRuntimeOnly("org.slf4j:slf4j-api:2.0.9")

    //todo wait for release
    testImplementation("com.intershop.gradle.test:test-gradle-plugin:4.1.2")
    testImplementation(gradleTestKit())

    testImplementation("commons-io:commons-io:2.14.0")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

