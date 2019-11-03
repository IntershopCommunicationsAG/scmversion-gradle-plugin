import com.jfrog.bintray.gradle.BintrayExtension
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import java.util.*

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
plugins {
    // build performance
    id("com.gradle.build-scan") version "3.0"

    // project plugins
    `java-gradle-plugin`
    groovy

    // test coverage
    jacoco

    // ide plugin
    idea

    // publish plugin
    `maven-publish`

    // plugin for documentation
    id("org.asciidoctor.jvm.convert") version "2.3.0"

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "0.10.1"

    // plugin for publishing to jcenter
    id("com.jfrog.bintray") version "1.8.4"
}

buildScan {
    termsOfServiceUrl   = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}

// release configuration
group = "com.intershop.gradle.scm"
description = "Gradle SCM version plugin - SCM based version handling for Gradle"
version = "6.0.0"

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

// test configuration
tasks {
    withType<Test>().configureEach {
        testLogging.showStandardStreams = false

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
        systemProperty("intershop.gradle.versions", "5.6.3")
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

    getByName("bintrayUpload")?.dependsOn("asciidoctor")
    getByName("publishToMavenLocal")?.dependsOn("asciidoctor")

    val sourcesJar = task<Jar>("sourceJar") {
        description = "Creates a JAR that contains the source code."

        from(sourceSets.getByName("main").allSource)
        archiveClassifier.set("sources")
    }

    val javadocJar = task<Jar>("javadocJar") {
        dependsOn("javadoc")
        description = "Creates a JAR that contains the javadocs."
        from(javadoc)
        archiveClassifier.set("javadoc")
    }
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])
            artifact(tasks.getByName("sourceJar"))
            artifact(tasks.getByName("javadocJar"))

            artifact(File(buildDir, "docs/asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(buildDir, "docs/asciidoc/docbook/README.xml")) {
                classifier = "docbook"
            }

            pom.withXml {
                val root = asNode()
                root.appendNode("name", project.name)
                root.appendNode("description", project.description)
                root.appendNode("url", "https://github.com/IntershopCommunicationsAG/${project.name}")

                val scm = root.appendNode( "scm" )
                scm.appendNode( "url", "https://github.com/IntershopCommunicationsAG/${project.name}")
                scm.appendNode( "connection", "git@github.com:IntershopCommunicationsAG/${project.name}.git")

                val org = root.appendNode( "organization" )
                org.appendNode( "name", "Intershop Communications" )
                org.appendNode( "url", "http://intershop.com" )

                val license = root.appendNode( "licenses" ).appendNode( "license" )
                license.appendNode( "name", "Apache License, Version 2.0" )
                license.appendNode( "url", "http://www.apache.org/licenses/LICENSE-2.0" )
                license.appendNode( "distribution", "repo" )
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_KEY")

    setPublications("intershopMvn")

    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = project.name
        userOrg = "intershopcommunicationsag"

        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"

        desc = project.description
        websiteUrl = "https://github.com/IntershopCommunicationsAG/${project.name}"
        issueTrackerUrl = "https://github.com/IntershopCommunicationsAG/${project.name}/issues"

        setLabels("intershop", "gradle", "plugin", "version", "release")
        publicDownloadNumbers = true

        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version.toString()
            desc = "${project.description} ${project.version}"
            released  = Date().toString()
            vcsTag = project.version.toString()
        })
    })
}

dependencies {
    implementation("com.intershop.gradle.version:extended-version:3.0.1")

    //jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.5.1.201910021850-r") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    testRuntimeOnly("org.apache.httpcomponents:httpclient:4.5.6")
    testRuntimeOnly("org.slf4j:slf4j-api:1.7.25")

    testImplementation("com.intershop.gradle.test:test-gradle-plugin:3.1.0-dev.2")
    testImplementation(gradleTestKit())

    testImplementation("commons-io:commons-io:2.2")
}

repositories {
    jcenter()
}

