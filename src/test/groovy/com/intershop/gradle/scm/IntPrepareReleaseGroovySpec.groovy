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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Slf4j
@Unroll
class IntPrepareReleaseGroovySpec extends AbstractTaskGroovySpec {

    final static String LOGLEVEL = "-i"

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    @Ignore("Needs be fixed with AB#100505")
    def 'test prepare release from trunk on GIT - tag does not exists - #gradleVersion'(gradleVersion) {
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
                .withArguments('showVersion', '--stacktrace', "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-SNAPSHOT')

        when:
        def prepareResult = getPreparedGradleRunner()
                .withArguments('release', '--stacktrace', "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        prepareResult.task(":release").outcome
        prepareResult.output.contains('Project version: 2.1.0')

        cleanup:
        gitTagRemove(testProjectDir, 'SBRELEASE_2.1.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    @Ignore("Needs be fixed with AB#100505")
    def 'test prepare release from trunk on GIT - tag does exists - #gradleVersion'(gradleVersion) {
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
        createResult.task(":tag").outcome == SUCCESS
        createResult.output.contains('Tag created: 2.1.0')

        when:
        def prepareResult = getPreparedGradleRunner()
                .withArguments('release', '--stacktrace', LOGLEVEL, "-DscmUserName=${System.properties['gituser']}", "-DscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        prepareResult.task(":release").outcome == SUCCESS
        prepareResult.output.contains('Project version: 2.1.0')

        cleanup:
        gitTagRemove(testProjectDir, 'SBRELEASE_2.1.0')

        where:
        gradleVersion << supportedGradleVersions
    }
}
