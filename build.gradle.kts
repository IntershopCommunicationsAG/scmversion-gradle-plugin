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
import io.gitee.pkmer.enums.PublishingType

plugins {

    // project plugins
    `jvm-test-suite`
    groovy

    kotlin("jvm") version "1.9.25" // A dependency on the standard library (stdlib) is added automatically to each source set.

    // test coverage
    jacoco

    // ide plugin
    idea

    // artifact signing - necessary on Maven Central
    signing

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "4.0.3"

    // documentation
    id("org.jetbrains.dokka") version "1.9.20"

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "1.3.0"

    id("io.gitee.pkmer.pkmerboot-central-publisher") version "1.1.1"
}

// release configuration
group = "com.intershop.gradle.scm"
description = "Gradle SCM version plugin - SCM based version handling for Gradle"
// apply gradle property 'projectVersion' to project.version, default to 'LOCAL'
val projectVersion : String? by project
version = projectVersion ?: "LOCAL"

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

gradlePlugin {
    plugins {
        website = "https://github.com/IntershopCommunicationsAG/${project.name}"
        vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
        create("scmversionPlugin") {
            id = "com.intershop.gradle.scmversion"
            implementationClass = "com.intershop.gradle.scm.ScmVersionPlugin"
            displayName = project.name
            description = project.description
            tags = listOf("intershop", "version", "release")
        }
    }
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot"
}

testing {
    suites.withType<JvmTestSuite> {
        useSpock()
        dependencies {
            implementation("com.intershop.gradle.test:test-gradle-plugin:5.1.0")
            implementation("commons-io:commons-io:2.17.0")
            implementation(gradleTestKit())

            runtimeOnly("org.apache.httpcomponents:httpclient:4.5.14")
            runtimeOnly("org.slf4j:slf4j-api:2.0.16")
        }

        targets {
            all {
                testTask.configure {
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

                    // Change directory for gradle tests
                    systemProperty("org.gradle.native.dir", ".gradle")
                    // Set supported Gradle version
                    systemProperty("intershop.gradle.versions", "8.4,8.5,8.10.2")
                    // Working dir for tests
                    systemProperty("intershop.test.base.dir", project.layout.buildDirectory.get().dir("test-working").asFile.absolutePath)

                    testLogging {
                        showStandardStreams = false
                        maxParallelForks = 1
                    }
                }
            }
        }
    }
}

tasks {
    register<Copy>("copyAsciiDoc") {
        includeEmptyDirs = false

        val outputDir = project.layout.buildDirectory.dir("tmp/asciidoctorSrc")
        val inputFiles = fileTree(mapOf("dir" to rootDir,
                "include" to listOf("**/*.asciidoc"),
                "exclude" to listOf("build/**")))

        inputs.files.plus(inputFiles)
        outputs.dir(outputDir)

        doFirst {
            outputDir.get().asFile.mkdir()
        }

        from(inputFiles)
        into(outputDir)
    }

    withType<AsciidoctorTask> {
        dependsOn("copyAsciiDoc")

        setSourceDir(project.layout.buildDirectory.dir("tmp/asciidoctorSrc"))
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        outputOptions {
            setBackends(listOf("html5", "docbook"))
        }

        setOptions(mapOf(
            "doctype"               to "article",
            "ruby"                  to "erubis"
        ))
        setAttributes(mapOf(
            "latestRevision"        to project.version,
            "toc"                   to "left",
            "toclevels"             to "2",
            "source-highlighter"    to "coderay",
            "icons"                 to "font",
            "setanchors"            to "true",
            "idprefix"              to "asciidoc",
            "idseparator"           to "-",
            "docinfo1"              to "true"
        ))
    }

    withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)

            html.outputLocation.set(project.project.layout.buildDirectory.dir("jacocoHtml"))
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    jar.configure {
        dependsOn("asciidoctor")
    }

    dokkaJavadoc.configure {
        outputDirectory.set(project.layout.buildDirectory.dir("dokka"))
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

val stagingRepoDir = project.layout.buildDirectory.dir("stagingRepo")

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])

            artifact(project.layout.buildDirectory.file("docs/asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(project.layout.buildDirectory.file("docs/asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }
        }
        withType<MavenPublication>().configureEach {
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
            name = "LOCAL"
            url = stagingRepoDir.get().asFile.toURI()
        }
    }
}

pkmerBoot {
    sonatypeMavenCentral{
        // the same with publishing.repositories.maven.url in the configuration.
        stagingRepository = stagingRepoDir

        /**
         * get username and password from
         * <a href="https://central.sonatype.com/account"> central sonatype account</a>
         */
        username = sonatypeUsername
        password = sonatypePassword

        // Optional the publishingType default value is PublishingType.AUTOMATIC
        publishingType = PublishingType.USER_MANAGED
    }
}

signing {
    sign(publishing.publications["intershopMvn"])
}

dependencies {
    implementation("com.intershop.gradle.version:extended-version:3.1.0")
    implementation(gradleKotlinDsl())

    //jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:6.10.0.202406032230-r")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
}

