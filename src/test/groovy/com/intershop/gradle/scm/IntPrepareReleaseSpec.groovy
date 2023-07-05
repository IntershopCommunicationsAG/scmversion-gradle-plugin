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

import com.intershop.gradle.scm.test.utils.AbstractTaskSpec
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Slf4j
@Unroll
class IntPrepareReleaseSpec extends AbstractTaskSpec {

    final static String LOGLEVEL = "-i"

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test prepare release from trunk on SVN - tag does not exists - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

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
                .withArguments('showVersion', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.3.0-SNAPSHOT')

        when:
        def createResult = getPreparedGradleRunner()
                .withArguments('release', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .withDebug(true)
                .build()

        then:
        createResult.task(":release").outcome
        createResult.output.contains('Version 2.3.0 will be used without extension')
        createResult.output.contains('Project version: 2.3.0')

        cleanup:
        svnRemove("${System.properties['svnurl']}/tags/SBRELEASE_2.3.0")

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test prepare release from trunk on SVN - tag does exists - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

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
                .withArguments('showVersion', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.3.0-SNAPSHOT')

        when:
        def createResult = getPreparedGradleRunner()
                .withArguments('tag', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        createResult.task(":tag").outcome
        createResult.output.contains('Tag created: 2.3.0')

        when:
        def prepareResult = getPreparedGradleRunner()
                .withArguments('release', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        prepareResult.task(":release").outcome
        prepareResult.output.contains('Project version: 2.3.0')

        cleanup:
        svnRemove("${System.properties['svnurl']}/tags/SBRELEASE_2.3.0")

        where:
        gradleVersion << supportedGradleVersions
    }

    /**
    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
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
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-SNAPSHOT')

        when:
        def prepareResult = getPreparedGradleRunner()
                .withArguments('release', '-PrunOnCI=true', '--stacktrace', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
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
 **/

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
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
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-SNAPSHOT')

        when:
        def createResult = getPreparedGradleRunner()
                .withArguments('tag', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        createResult.task(":tag").outcome == SUCCESS
        createResult.output.contains('Tag created: 2.1.0')

        when:
        def prepareResult = getPreparedGradleRunner()
                .withArguments('release', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
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
