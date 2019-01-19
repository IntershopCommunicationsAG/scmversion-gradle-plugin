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
import spock.lang.Requires
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Slf4j
@Unroll
class IntToVersionSpec extends AbstractTaskSpec {

    final static String LOGLEVEL = "-i"

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test toVersion task with svn trunk - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        initSettingsFile()
        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
            version {
                patternDigits = 2
                versionBranch = 'branch'
            }
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def preResult = getPreparedGradleRunner()
                .withArguments('showVersion', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        preResult.task(":showVersion").outcome == SUCCESS
        preResult.output.contains('Project version: 2.1.0-SNAPSHOT')

        when:
        def result = getPreparedGradleRunner()
                .withArguments('toVersion', '-PtargetVersion=1.1.0', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(':toVersion').outcome == SUCCESS
        result.output.contains('Working copy was switched to')

        when:
        def postResult = getPreparedGradleRunner()
                .withArguments('showVersion', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        postResult.task(":showVersion").outcome == SUCCESS
        postResult.output.contains('Project version: 1.1.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    //def 'test toVersion task with git master - #gradleVersion'(gradleVersion) {
    def 'test toVersion task with git master - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'].toString(), 'master' )
        initSettingsFile()

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm .prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def preResult01 = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        preResult01.task(":showVersion").outcome == SUCCESS
        preResult01.output.contains('Project version: 2.1.0-SNAPSHOT')

        when:
        def result01 = getPreparedGradleRunner()
                .withArguments('toVersion', '-PtargetVersion=1.1.0', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result01.task(':toVersion').outcome == SUCCESS

        when:
        def postResult01 = getPreparedGradleRunner()
                .withArguments('showVersion', '-PrunOnCI=true', '--stacktrace', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        postResult01.task(":showVersion").outcome == SUCCESS
        postResult01.output.contains('Project version: 1.1.0')

        when:
        def result02 = getPreparedGradleRunner()
                .withArguments('toVersion', '-PtargetVersion=4.1.0', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .buildAndFail()

        then:
        result02.task(':toVersion').outcome == FAILED
        result02.output.contains("Version '4.1.0' does not exists")

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test toVersion task with git feature branch - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'].toString(), 'master' )

        initSettingsFile()
        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm .prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def preResult01 = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        preResult01.task(":showVersion").outcome == SUCCESS
        preResult01.output.contains('Project version: 2.1.0-SNAPSHOT')

        when:
        def result01 = getPreparedGradleRunner()
                .withArguments('toVersion', '-PtargetVersion=1.1.0', '-Pfeature=test', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result01.task(':toVersion').outcome == SUCCESS

        where:
        gradleVersion << supportedGradleVersions
    }

}
