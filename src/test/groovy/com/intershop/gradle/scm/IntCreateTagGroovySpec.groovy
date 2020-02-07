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
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Slf4j
@Unroll
class IntCreateTagGroovySpec extends AbstractTaskGroovySpec {

    final static String LOGLEVEL = "-i"

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test tag creation from trunk on Git - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-SNAPSHOT')

        when:
        def createResult = getPreparedGradleRunner()
                .withArguments('tag', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        createResult.task(":tag").outcome
        createResult.output.contains('Tag created: 2.1.0')

        when:
        changeTestFile(testProjectDir)
        def changeResult = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        changeResult.task(":showVersion").outcome == SUCCESS
        changeResult.output.contains('Project version: 2.1.1-SNAPSHOT')

        cleanup:
        gitTagRemove(testProjectDir, 'SBRELEASE_2.1.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test tag creation from branch on Git - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-SNAPSHOT')

        when:
        def createResult = getPreparedGradleRunner()
                .withArguments('tag', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        createResult.task(":tag").outcome
        createResult.output.contains('Tag created: 1.1.1')


        when:
        gitChangeTestFile(testProjectDir)
        def changeResult = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        changeResult.task(":showVersion").outcome == SUCCESS
        changeResult.output.contains('Project version: 1.1.2-SNAPSHOT')

        cleanup:
        gitTagRemove(testProjectDir, 'SBRELEASE_1.1.1')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test tag creation from trunk on Git - increment minor - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-SNAPSHOT')

        when:
        def createResult = getPreparedGradleRunner()
                .withArguments('tag', '-Pincrement=MINOR', '--stacktrace', LOGLEVEL, "-DINCREMENT=MINOR", "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        createResult.task(":tag").outcome
        createResult.output.contains('Tag created: 1.2.0')

        cleanup:
        gitTagRemove(testProjectDir, 'SBRELEASE_1.2.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    //test class loader issues
    @Ignore
    def 'test tag creation from trunk on Git - without username and password - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('tag', '--stacktrace', '-Pincrement=MINOR', '--stacktrace', LOGLEVEL)
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result.task(":tag").outcome == FAILED
        result.output.contains('Authentication is required but no CredentialsProvider has been registered')

        where:
        gradleVersion << supportedGradleVersions
    }
}
