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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Slf4j
@Unroll
class IntShowVersionSpec extends AbstractTaskSpec {

    final static String LOGLEVEL = "-i"

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test static version property'() {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
            id 'java'
        }

        scm {
            prefixes {
                tagPrefix = 'ORELEASE'
            }
        }

        version = scm.version.version

        println "version is : \${scm.version.version}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', "-PstaticVersion=FBNAMEVER")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: FBNAMEVER')
        result.output.contains('version is : FBNAMEVER')

        when:
        def resultwop = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultwop.task(":showVersion").outcome == SUCCESS
        resultwop.output.contains('Project version: FBNAMEVER')
        resultwop.output.contains('version is : FBNAMEVER')

        when:
        def resultsvn = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', "-PstaticVersion=")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultsvn.task(":showVersion").outcome == SUCCESS
        resultsvn.output.contains('Project version: 1.0.0-LOCAL')
        resultsvn.output.contains('version is : 1.0.0-LOCAL')

        when:
        def resultn = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', "-PstaticVersion=FBNAMEVER")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultn.task(":showVersion").outcome == SUCCESS
        resultn.output.contains('Project version: FBNAMEVER')
        resultn.output.contains('version is : FBNAMEVER')

        when:
        def resultc = getPreparedGradleRunner()
                .withArguments('clean', '--stacktrace')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultc.task(":clean").outcome == SUCCESS

        when:
        def resultv = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        resultv.task(":showVersion").outcome == SUCCESS
        resultv.output.contains('Project version: 1.0.0-LOCAL')
        resultv.output.contains('version is : 1.0.0-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn trunk - default version - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'ORELEASE'
            }
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0-LOCAL')
        result.output.contains('branchname: trunk')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn trunk - default version with four digits - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            version {
                type = 'fourDigits'
                initialVersion = '1.0.0.0'
            }
            prefixes {
                tagPrefix = 'ORELEASE'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0.0-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn trunk and special prefix configuration - #gradleVersion'(gradleVersion) {
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
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.3.0-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn trunk and special prefix configuration on CI - #gradleVersion'(gradleVersion) {
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

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn trunk and version from branches - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.version {
            versionBranch = 'branch'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn trunk and version from branches with local changes - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")
        svnChangeTestFile(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.version {
            versionBranch = 'branch'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn branch - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/branches/SB_2.0")

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
            }
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.1-LOCAL')
        result.output.contains('branchname: SB_2.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn feature branch - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/branches/FB_1.0.0-fb-123")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
            version {
                versionBranch = 'branch'
            }
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0-fb-123-LOCAL')
        result.output.contains('branchname: FB_1.0.0-fb-123')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn bugfix branch - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/branches/BB_2.0.0-ISTOOL-12345")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                bugfixPrefix = 'BB'
            }
            version {
                versionBranch = 'branch'
            }
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-ISTOOL-12345-LOCAL')
        result.output.contains('branchname: BB_2.0.0-ISTOOL-12345')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn hotfix branch - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/branches/HB_2.0.0-ISTOOL-1234")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                hotfixPrefix = 'HB'
            }
            version {
                versionBranch = 'branch'
            }
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-ISTOOL-1234-LOCAL')
        result.output.contains('branchname: HB_2.0.0-ISTOOL-1234')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn feature branch - jira issue included - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/branches/FB_1.0.0-fb-123")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
            version {
                versionBranch = 'branch'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0-fb-123-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn tag - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/tags/SBRELEASE_1.1.0")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
            version {
                versionBranch = 'branch'
            }
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.0-LOCAL')
        result.output.contains('branchname: SBRELEASE_1.1.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn tag on CI server - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/tags/SBRELEASE_1.1.0")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'SBRELEASE'
            }
            version {
                versionBranch = 'branch'
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
        result.output.contains('Project version: 1.1.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn tag from feauture branch on CI server - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/tags/RELEASE_1.0.0-fb-123-dev1")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'RELEASE'
            }

            //version {
            //    versionBranch = 'branch'
            //}
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
        result.output.contains('Project version: 1.0.0-fb-123-dev1')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn tag from feauture branch with short version number on CI server - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/tags/RELEASE_1-fb-123-dev1")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'RELEASE'
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
        result.output.contains('Project version: 1.0.0-fb-123-dev1')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task on CI server with useBuildExtension - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'CIRELEASE'
            }
            version {
                useBuildExtension = 'true'
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
        result.output.contains('Project version: 1.0.0-dev.2-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task on CI server without useBuildExtension - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'CIRELEASE'
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
        result.output.contains('Project version: 1.0.0-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task and previous version on svn - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/tags/CLRELEASE_2.0.0")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'CLRELEASE'
            }
        }

        version = scm.version.version

        println "***previuous version is \${scm.version.getPreviousVersion()} ***"

        println "branchname: \${scm.version.branchName}"
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .withDebug(true)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0')
        result.output.contains('***previuous version is 1.5.0 ***')
        result.output.contains('branchname: CLRELEASE_2.0.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task with disableSCM - #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            version{
                disableSCM = true
                initialVersion = '2.0.0'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task on CI server with disableSCM (prerelease)- #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            version{
                disableSCM = true
                initialVersion = '2.0.0'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL,'-PrunOnCI=true', "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-20')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task on CI server with disableSCM (release)- #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            version{
                disableSCM = true
                initialVersion = '2.0.0'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL,'-PrunOnCI=true', "-PscmVersionExt=RELEASE", "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0')
        ! result.output.contains('Project version: 2.0.0-')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task on CI server with disableSCM (snapshot)- #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            version{
                disableSCM = true
                initialVersion = '2.0.0'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL,'-PrunOnCI=true', "-PscmVersionExt=SNAPSHOT", "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task on CI server with disableSCM (other version)- #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            version{
                disableSCM = true
                initialVersion = 'ab.cd.de.00'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmVersionExt=SNAPSHOT", "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: ab.cd.de.00-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git trunk - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-LOCAL')
        result.output.contains('branchname: master')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git trunk without user name and password - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git trunk on CI - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git master and tag - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'TRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.4.0-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git trunk on CI without user name and password - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-LOCAL')
        result.output.contains('branchname: SB_1.1')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git bugfix branch - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'BB_2.0-JIRA-4712' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            bugfixPrefix = 'BB'
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-JIRA-4712-LOCAL')
        result.output.contains('branchname: BB_2.0-JIRA-4712')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git bugfix branch and without version - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'BB_JIRA-4712' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }
        scm {
            prefixes {
                bugfixPrefix = 'BB'
                tagPrefix = 'SBRELEASE'
            }
        }
        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .withDebug(true)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-JIRA-4712-LOCAL')
        result.output.contains('branchname: BB_JIRA-4712')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git bugfix branch and without version and increased major version - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'BB_JIRA-4712' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }
        scm {
            prefixes {
                bugfixPrefix = 'BB'
                tagPrefix = 'SBRELEASE'
            }
            version {
                increment = 'MAJOR'
            }
        }
        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 3.0.0-JIRA-4712-LOCAL')
        result.output.contains('branchname: BB_JIRA-4712')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git bugfix branch and without version and not increased major version - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'BB_JIRA-4712' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }
        scm {
            prefixes {
                bugfixPrefix = 'BB'
                tagPrefix = 'TRELEASE'
            }
            version {
                increment = 'HOTFIX'
            }
        }
        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-JIRA-4712-LOCAL')
        result.output.contains('branchname: BB_JIRA-4712')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git hotfix branch - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'HB_2.0-JIRA-4711' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            hotfixPrefix = 'HB'
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-JIRA-4711-LOCAL')
        result.output.contains('branchname: HB_2.0-JIRA-4711')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch without user name and password - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch with local changes - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1' )
        gitChangeTestFile(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-LOCAL')
        result.output.contains('This file is not indexed new.properties')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch with local changes without user name and passwords - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1' )
        gitChangeTestFile(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch on CI server - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_2.0' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        println "branchname: \${scm.version.branchName}"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0')
        result.output.contains('branchname: SBRELEASE_2.0.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch on CI server without user name and password - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_2.0' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch with local changes on CI server - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1' )
        gitChangeTestFile(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes {
            tagPrefix = 'SBRELEASE'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch with local changes on CI server with useBuildExtension - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )
        gitChangeTestFile(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'CIRELEASE'
            }
            version {
                useBuildExtension = true
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0-dev.2-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch with local changes on CI server without useBuildExtension - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )
        gitChangeTestFile(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'CIRELEASE'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task and previous version on git - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'CLRELEASE'
            }
        }

        version = scm.version.version

        println "***previuous version is \${scm.version.previousVersion}***"

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '-PrunOnCI=true', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-SNAPSHOT')
        result.output.contains('***previuous version is 2.0.0***')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git branch with local changes on CI server without user name and password - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'SB_1.1' )
        gitChangeTestFile(testProjectDir)

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
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, '-PrunOnCI=true')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn trunk - offline - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', '-PofflineVersion=9.0.0.0-LOCAL')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 9.0.0.0-LOCAL')

        where:
        gradleVersion << supportedGradleVersions
    }


    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test showVersion task with svn trunk - continuousRelease - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            version {
                continuousRelease = true
            }
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0-fb-123-rev.id.')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git master - continuousRelease - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'master' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'CTRELEASE'
            }
            version {
                continuousRelease = true
            }
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.2.0-rev.id.')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git and unspecified branch - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'IS-2177-testbranch' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }
        
        scm {
            prefixes {
                tagPrefix = 'BRELEASE'
            }
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true', LOGLEVEL)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-IS-2177-testbranch-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'test showVersion task with git and unspecified branch and no version on branch - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCheckout(testProjectDir, System.properties['giturl'], 'IS-2177-testbranch' )

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true', LOGLEVEL)
                .withDebug(true)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0-IS-2177-testbranch-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task with git - checkout commit - #gradleVersion'(gradleVersion) {
        given:
        prepareGitCommitCheckout(testProjectDir, System.properties['giturl'], 'ad73b690ccfbc5d59eec6597073bd6c24aee6519')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', '-PrunOnCI=true', LOGLEVEL)
                .withDebug(true)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.0.0-rev.id.ad73b69-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }
}
