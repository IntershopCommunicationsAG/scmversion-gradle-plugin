import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.BintrayUploadTask
import com.gradle.publish.PluginBundleExtension
import org.asciidoctor.gradle.AsciidoctorTask
import org.asciidoctor.gradle.AsciidoctorExtension
import java.util.Date

plugins {
    // project plugins
    `java-gradle-plugin`
    groovy

    // test coverage
    jacoco

    // ide plugin
    idea

    // plugin for documentation
    id("org.asciidoctor.convert") version "1.5.9.2"

    // publish plugin
    `maven-publish`

    // plugin for publishing to Gradle Portal
    id("com.gradle.plugin-publish") version "0.10.0"

    // plugin for publishing to jcenter
    id("com.jfrog.bintray") version "1.8.4"
}

// release configuration
group = "com.intershop.gradle.scm"
description = "Gradle SCM version plugin - SCM based version handling for Gradle"

version = "4.1.0"

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
tasks.withType<Test>().configureEach {
    testLogging.showStandardStreams = true

    systemProperty("IDE_TEST_DEBUG_SUPPORT", "true")

    if(! System.getenv("SVNUSER").isNullOrBlank() &&
            ! System.getenv("SVNPASSWD").isNullOrBlank() &&
            ! System.getenv("SVNURL").isNullOrBlank()) {
        systemProperty("svnurl", System.getenv("SVNURL"))
        systemProperty("svnuser", System.getenv("SVNUSER"))
        systemProperty("svnpasswd", System.getenv("SVNPASSWD"))
    }

    if(! System.getenv("GITUSER").isNullOrBlank() &&
            ! System.getenv("GITPASSWD").isNullOrBlank() &&
            ! System.getenv("GITURL").isNullOrBlank()) {
        systemProperty("giturl", System.getenv("GITURL"))
        systemProperty("gituser", System.getenv("GITUSER"))
        systemProperty("gitpasswd", System.getenv("GITPASSWD"))
    }

    if(! System.getProperty("SVNUSER").isNullOrBlank() &&
            ! System.getProperty("SVNPASSWD").isNullOrBlank() &&
            ! System.getProperty("SVNURL").isNullOrBlank()) {
        systemProperty("svnurl", System.getProperty("SVNURL"))
        systemProperty("svnuser", System.getProperty("SVNUSER"))
        systemProperty("svnpasswd", System.getProperty("SVNPASSWD"))
    }

    if(! System.getProperty("GITUSER").isNullOrBlank() &&
            ! System.getProperty("GITPASSWD").isNullOrBlank() &&
            ! System.getProperty("GITURL").isNullOrBlank() ) {
        systemProperty("giturl", System.getProperty("GITURL"))
        systemProperty("gituser", System.getProperty("GITUSER"))
        systemProperty("gitpasswd", System.getProperty("GITPASSWD"))
    }

    //Change directory for gradle tests
    systemProperty("org.gradle.native.dir", ".gradle")
    //Set supported Gradle version
    systemProperty("intershop.gradle.versions", "4.9,5.1")
    //working dir for tests
    systemProperty("intershop.test.base.dir", (File(project.buildDir, "test-working")).absolutePath)
}

task("copyAsciiDoc") {

    val outputDir = file("${buildDir}/tmp/asciidoctorSrc")
    val inputFiles = fileTree(mapOf("dir" to rootDir, "include" to listOf("**/*.asciidoc")))

    inputs.files.plus( inputFiles )
    outputs.dir( outputDir )

    doLast {
        outputDir.mkdir()

        project.copy {
            from(inputFiles)
            into(outputDir)
        }
    }
}

configure<AsciidoctorExtension> {
    noDefaultRepositories = true
}

tasks {
    withType<AsciidoctorTask> {
        dependsOn("copyAsciiDoc")

        sourceDir = file("${buildDir}/tmp/asciidoctorSrc")
        sources(delegateClosureOf<PatternSet> {
            include("README.asciidoc")
        })

        backends("html5", "docbook")
        options = mapOf( "doctype" to "article",
                "ruby"    to "erubis")
        attributes = mapOf(
                "latestRevision"        to  project.version,
                "toc"                   to "left",
                "toclevels"             to "2",
                "source-highlighter"    to "coderay",
                "icons"                 to "font",
                "setanchors"            to "true",
                "idprefix"              to "asciidoc",
                "idseparator"           to "-",
                "docinfo1"              to "true")
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

    withType<Test> {
        dependsOn("jar")
    }

    getByName("bintrayUpload")?.dependsOn("asciidoctor")
    getByName("publishToMavenLocal")?.dependsOn("asciidoctor")
}

val sourcesJar = task<Jar>("sourceJar") {
    description = "Creates a JAR that contains the source code."

    from(sourceSets.getByName("main").allSource)
    classifier = "sources"
}

val groovydocJar = task<Jar>("javadocJar") {
    dependsOn("groovydoc")
    description = "Creates a JAR that contains the javadocs."

    from(tasks.getByName("groovydoc"))
    classifier = "javadoc"
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])
            artifact(sourcesJar)
            artifact(groovydocJar)

            artifact(File(buildDir, "asciidoc/html5/README.html")) {
                classifier = "reference"
            }

            artifact(File(buildDir, "asciidoc/docbook/README.xml")) {
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

                val license = root.appendNode( "licenses" ).appendNode( "license" );
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

    //svn
    // replace svnkit's JNA 4.x with 3.2.7, which is used by Gradle itself
    implementation("org.tmatesoft.svnkit:svnkit:1.9.3") {
        exclude(group = "net.java.dev.jna")
        exclude(group = "com.trilead", module = "trilead-ssh2")
    }
    implementation("com.trilead:trilead-ssh2:1.0.0-build221")
    testRuntimeOnly("net.java.dev.jna:jna:4.1.0")

    //jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.1.3.201810200350-r") {
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
