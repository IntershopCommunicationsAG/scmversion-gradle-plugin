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
 *  limitations under the License.
 */

package com.intershop.gradle.scm

import com.intershop.gradle.scm.test.utils.AbstractTaskGroovySpec
import groovy.util.logging.Slf4j
import spock.lang.Requires
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Slf4j
@Unroll
class IntChangelogGroovySpec extends AbstractTaskGroovySpec {

    final static String LOGLEVEL = "-info"

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test create changelog for git - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File testLog = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        testLog.exists()
        testLog.text.contains('add change')
        testLog.text.contains('| M | test.properties')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test create changelog for git without previous version - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'OLRELEASE'

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File f = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        f.exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test create changelog for git with special target version - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'

        version = scm.version.version

        scm.changelog.previousVersion = '1.0.0'

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File f = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        f.exists()
        f.text.contains('This list contains changes since 1.0.0.')
        f.text.contains('add change on master after CL 2.0.0')
        f.text.contains('| M | test.properties')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test changelog output for git - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'

        version = scm.version.version

        task copy(type: Copy) {
            // Copy the output of changelog
            from changelog.outputs.files
            into project.buildDir
        }

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copy', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File f = new File(testProjectDir, 'build/changelog/changelog.asciidoc')
        File cf = new File(testProjectDir, 'build/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        f.exists()
        cf.exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test changelog output with changed outputfile for git - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'
        scm.changelog.changelogFile = new File(project.buildDir, 'testlog/testlog.asciidoc')

        version = scm.version.version

        task copy(type: Copy) {
            // Copy the output of changelog
            from changelog.outputs.files
            into project.buildDir
        }

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copy', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File f = new File(testProjectDir, 'build/testlog/testlog.asciidoc')
        File cf = new File(testProjectDir, 'build/testlog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        f.exists()
        cf.exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'get change log on git between tag and not configured previous tag'() {
        given:
        prepareTagGitCheckout(testProjectDir, System.properties['giturl'],'CLRELEASE_2.0.0')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'

        version = scm.version.version

        """.stripIndent()



        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File testLog = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        testLog.exists()
        testLog.text.contains('This list contains changes since 1.5.0.')
        testLog.text.contains('add change for CL')
        testLog.text.contains('| M | test.properties')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'get change log on git between tag and configured tag'() {
        given:
        prepareTagGitCheckout(testProjectDir, System.properties['giturl'],'CLRELEASE_2.0.0')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--prevVersion=1.0.0', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File testLog = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        testLog.exists()
        testLog.text.contains('This list contains changes since 1.0.0.')
        testLog.text.contains('add change for CL')
        testLog.text.contains('| M | test.properties')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'configured previous version does not exists'() {
        given:
        prepareTagGitCheckout(testProjectDir, System.properties['giturl'],'CLRELEASE_2.0.0')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--prevVersion=0.9.0', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result.output.contains("The configured previous version is not available! Please check your configuration.")

        where:
        gradleVersion << supportedGradleVersions
    }
}
