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
class IntChangelogSpec extends AbstractTaskSpec {

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test create changelog for svn - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--stacktrace', '-i', "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File f = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        f.exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test create changelog for svn without previous version - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/tags/SBRELEASE_1.1.0")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'SBRELEASE'

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--stacktrace', '-i', "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File f = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        f.exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test create changelog for svn with special target version - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            prefixes {
                tagPrefix = 'CLRELEASE'
            }
            changelog {
                targetVersion = '1.0.0'
            }
        }

        version = scm.version.version

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--stacktrace', '-i', "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnuserpwd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File f = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        f.exists()
        f.text.contains('This list contains changes since version 1.0.0.')
        f.text.contains('JIRA-12345: add test property to file')
        f.text.contains('| M | /trunk/test.properties')

        where:
        gradleVersion << supportedGradleVersions
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test changelog output for svn - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

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

        task copyDoc {
            inputs.file changelog
            doLast{
                copy {
                    // Copy the output of copyTask
                    from changelog
                    into project.buildDir
                }
            }
        }

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyDoc', '--stacktrace', '-i', "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnuserpwd']}")
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

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'test changelog output with changed outputfile for svn - #gradleVersion'(gradleVersion) {
        given:
        svnCheckOut(testProjectDir, "${System.properties['svnurl']}/trunk")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm.prefixes.tagPrefix = 'CLRELEASE'
        scm.changelog.changelogFile = new File(project.buildDir, 'testlog/testlog.asciidoc')

        version = scm.version.version

        task copyDoc {
            inputs.file changelog
            doLast{
                copy {
                    // Copy the output of copyTask
                    from changelog
                    into project.buildDir
                }
            }
        }

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyDoc', '--stacktrace', '-i', "-PscmUserName=${System.properties['svnuser']}", "-PscmUserPasswd=${System.properties['svnuserpwd']}")
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
                .withArguments('changelog', '--stacktrace', '-i', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
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
                .withArguments('changelog', '--stacktrace', '-i', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
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

        scm.changelog.targetVersion = '1.0.0'

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('changelog', '--stacktrace', '-i', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
                .withGradleVersion(gradleVersion)
                .build()
        File f = new File(testProjectDir, 'build/changelog/changelog.asciidoc')

        then:
        result.task(":changelog").outcome == SUCCESS
        f.exists()
        f.text.contains('This list contains changes since version 1.0.0.')
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

        task copyDoc {
            inputs.file changelog
            doLast{
                copy {
                    // Copy the output of copyTask
                    from changelog
                    into project.buildDir
                }
            }
        }

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyDoc', '--stacktrace', '-i', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
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

        task copyDoc {
            inputs.file changelog
            doLast{
                copy {
                    // Copy the output of copyTask
                    from changelog
                    into project.buildDir
                }
            }
        }

        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyDoc', '--stacktrace', '-i', "-PscmUserName=${System.properties['gituser']}", "-PscmUserPasswd=${System.properties['gitpasswd']}")
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
}
