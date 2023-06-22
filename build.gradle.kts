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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {

    // project plugins
    `java-gradle-plugin`
    groovy

    kotlin("jvm") version "1.3.72"

    // test coverage
    jacoco

    // ide plugin
    idea

    // publish plugin
    `maven-publish`

    // artifact signing - necessary on Maven Central
    signing

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "3.3.2"

    // documentation
    id("org.jetbrains.dokka") version "1.5.0"

    // code analysis for kotlin
    id("io.gitlab.arturbosch.detekt") version "1.18.0"

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "1.0.0"
}

// release configuration
group = "com.intershop.gradle.scm"
description = "Gradle SCM version plugin - SCM based version handling for Gradle"
version = "6.2.1"

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val pluginId = "com.intershop.gradle.scmversion"

gradlePlugin {
    plugins {
        create("scmversionPlugin") {
            id = pluginId
            implementationClass = "com.intershop.gradle.scm.ScmVersionPlugin"
            displayName = project.name
            description = project.description
        }
    }
}

pluginBundle {
    website = "https://github.com/IntershopCommunicationsAG/${project.name}"
    vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
    tags = listOf("intershop", "gradle", "plugin", "version", "release")
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot'"
}

detekt {
    input = files("src/main/kotlin")
    config = files("detekt.yml")
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

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
        systemProperty("intershop.gradle.versions", "6.5,6.6")
        //working dir for tests
        systemProperty("intershop.test.base.dir", (File(project.buildDir, "test-working")).absolutePath)
    }

    val copyAsciiDoc = register<Copy>("copyAsciiDoc") {
        includeEmptyDirs = false

        val outputDir = file("$buildDir/tmp/asciidoctorSrc")
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

        setSourceDir(file("$buildDir/tmp/asciidoctorSrc"))
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
            xml.isEnabled = true
            html.isEnabled = true

            html.destination = File(project.buildDir, "jacocoHtml")
        }

        val jacocoTestReport by tasks
        jacocoTestReport.dependsOn("test")
    }

    getByName("jar").dependsOn("asciidoctor")

    val compileKotlin by getting(KotlinCompile::class) {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    dokkaJavadoc.configure {
        outputDirectory.set(buildDir.resolve("dokka"))
    }

    val sourcesJar = task<Jar>("sourceJar") {
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

            artifact(File(buildDir, "docs/asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(buildDir, "docs/asciidoc/docbook/README.xml")) {
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
    implementation(kotlin("stdlib-jdk8"))

    //jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.5.1.201910021850-r") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    testRuntimeOnly("org.apache.httpcomponents:httpclient:4.5.6")
    testRuntimeOnly("org.slf4j:slf4j-api:1.7.25")

    testImplementation("com.intershop.gradle.test:test-gradle-plugin:4.1.2")
    testImplementation(gradleTestKit())

    testImplementation("commons-io:commons-io:2.2")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

