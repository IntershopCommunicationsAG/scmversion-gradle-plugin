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

import com.intershop.gradle.scm.test.utils.AbstractTaskKtsSpec
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Requires
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Ignore
@Slf4j
@Unroll
class IntShowVersionKtsSpec extends AbstractTaskKtsSpec {

    final static String LOGLEVEL = "-i"

    def 'test showVersion task with disableSCM - #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id("com.intershop.gradle.scmversion")
        }

        scm {
            version{
                disableSCM = true
                initialVersion = "2.0.0"
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
        result.output.contains('Project version: 2.0.0-')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task on CI server with disableSCM (prerelease)- #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id("com.intershop.gradle.scmversion")
        }
        
        scm {
            version{
                disableSCM = true
                initialVersion = "2.0.0"
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
        result.output.contains('Project version: 2.0.0-20')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task on CI server with disableSCM (release)- #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id("com.intershop.gradle.scmversion")
        }
        
        scm {
            version{
                disableSCM = true
                initialVersion = "2.0.0"
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmVersionExt=RELEASE", "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
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
            id("com.intershop.gradle.scmversion")
        }

        scm {
            version{
                disableSCM = true
                initialVersion = "2.0.0"
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
        result.output.contains('Project version: 2.0.0-SNAPSHOT')

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'test showVersion task on CI server with disableSCM (other version)- #gradleVersion'(gradleVersion) {
        given:
        this.file('test.properties')

        buildFile << """
        plugins {
            id("com.intershop.gradle.scmversion")
        }

        scm {
            version{
                disableSCM = true
                initialVersion = "ab.cd.de.00"
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
        result.output.contains('Project version: ab.cd.de.00-SNAPSHOT')

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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
        }

        version = scm.version.version

        println("branchname: \${scm.version.branchName}")
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.1.0-SNAPSHOT')
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
            id("com.intershop.gradle.scmversion")
        }

        scm {
            prefixes {
                tagPrefix = "SBRELEASE"
            }
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
        result.output.contains('Project version: 2.1.0-SNAPSHOT')

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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "TRELEASE"
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
        }

        version = scm.version.version

        println("branchname: \${scm.version.branchName}")

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 1.1.1-SNAPSHOT')
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            bugfixPrefix = "BB"
        }

        version = scm.version.version

        println("branchname: \${scm.version.branchName}")

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-JIRA-4712-SNAPSHOT')
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
            id("com.intershop.gradle.scmversion")
        }
        scm {
            prefixes {
                bugfixPrefix = "BB"
                tagPrefix = "SBRELEASE"
            }
            version {
                increment = "MAJOR"
            }
        }
        version = scm.version.version

        println("branchname: \${scm.version.branchName}")
       
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .withDebug(true)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-JIRA-4712-SNAPSHOT')
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
            id("com.intershop.gradle.scmversion")
        }
        scm {
            prefixes {
                bugfixPrefix = "BB"
                tagPrefix = "SBRELEASE"
            }
            version {
                increment = "MAJOR"
            }
        }
        version = scm.version.version

        println("branchname: \${scm.version.branchName}")

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 3.0.0-JIRA-4712-SNAPSHOT')
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
            id("com.intershop.gradle.scmversion")
        }
        scm {
            prefixes {
                bugfixPrefix = "BB"
                tagPrefix = "TRELEASE"
            }
            version {
                increment = "HOTFIX"
            }
        }
        version = scm.version.version

        println("branchname: \${scm.version.branchName}")

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-JIRA-4712-SNAPSHOT')
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            hotfixPrefix = "HB"
        }

        version = scm.version.version

        println("branchname: \${scm.version.branchName}")

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 2.0.0-JIRA-4711-SNAPSHOT')
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
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
        result.output.contains('Project version: 1.1.1-SNAPSHOT')

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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
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
        result.output.contains('Project version: 1.1.1-SNAPSHOT')
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
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
        result.output.contains('Project version: 1.1.1-SNAPSHOT')

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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
        }

        version = scm.version.version

        println("branchname: \${scm.version.branchName}")

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
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
            id("com.intershop.gradle.scmversion")
        }

        scm.prefixes {
            tagPrefix = "SBRELEASE"
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
            id("com.intershop.gradle.scmversion")
        }

        scm {
            prefixes {
                tagPrefix = "CIRELEASE"
            }
            version {
                useBuildExtension = true
            }
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
            id("com.intershop.gradle.scmversion")
        }

        scm {
            prefixes {
                tagPrefix = "CIRELEASE"
            }
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
            id("com.intershop.gradle.scmversion")
        }

        scm {
            prefixes {
                tagPrefix = "CLRELEASE"
            }
        }

        version = scm.version.version

        println("***previuous version is \${scm.version.previousVersion}***")

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL, "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
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
            id("com.intershop.gradle.scmversion")
        }

        scm {
            prefixes {
                tagPrefix = "SBRELEASE"
            }
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
        result.output.contains('Project version: 1.1.1-SNAPSHOT')

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
            id("com.intershop.gradle.scmversion")
        }

        scm {
            prefixes {
                tagPrefix = "CTRELEASE"
            }
            version {
                continuousRelease = true
            }
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', "-i")
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS

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
            id("com.intershop.gradle.scmversion")
        }
        
        scm {
            prefixes {
                tagPrefix = "BRELEASE"
            }
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
            id("com.intershop.gradle.scmversion")
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL)
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
            id("com.intershop.gradle.scmversion")
        }
        
        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', "-d")
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
