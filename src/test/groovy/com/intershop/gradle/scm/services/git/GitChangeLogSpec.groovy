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
package com.intershop.gradle.scm.services.git

import com.intershop.gradle.scm.ScmVersionPlugin
import com.intershop.gradle.scm.builder.ScmBuilder
import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.services.ScmChangeLogService
import com.intershop.gradle.scm.test.utils.AbstractScmSpec
import com.intershop.gradle.test.util.TestDir
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Requires
import spock.lang.Unroll

@Unroll
class GitChangeLogSpec extends AbstractScmSpec {

    @TestDir
    File projectDir

    @TestDir
    File targetDir

    @Rule
    TestName testName = new TestName()

    protected String canonicalName
    protected Project project

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'get change log on git between head and tag'() {
        setup:
        project = prepareProject('master', '')
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog

        when:
        scls.createLog()

        then:
        testLog.exists()
        testLog.text.contains('add change')
        testLog.text.contains('| M | test.properties')
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'get change log on svn between tag and not configured previous tag'() {
        setup:
        project = prepareProject('master','CLRELEASE_2.0.0')
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog

        when:
        scls.createLog()

        then:
        testLog.exists()
        testLog.text.contains('This list contains changes since version 1.5.0.')
        testLog.text.contains('add change for CL')
        testLog.text.contains('| M | test.properties')
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'get change log on svn between tag and configured tag'() {
        setup:
        project = prepareProject('master', 'CLRELEASE_2.0.0')
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog
        scls.setTargetVersion('1.0.0')

        when:
        scls.createLog()

        then:
        testLog.exists()
        testLog.text.contains('This list contains changes since version 1.0.0.')
        testLog.text.contains('add change for CL')
        testLog.text.contains('| M | test.properties')
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'configured previous version does not exists'() {
        setup:
        project = prepareProject('master', 'CLRELEASE_2.0.0')
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog
        scls.targetVersion = '0.9.0'

        when:
        scls.createLog()

        then:
        testLog.exists()
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'there is no previous version tag'() {
        setup:
        project = prepareProject('master', 'CLRELEASE_1.0.0')
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog

        when:
        scls.createLog()

        then:
        testLog.exists()
    }

    private Project prepareProject(String branch, String tag) {
        File buildFile = new File(projectDir, 'build.gradle')
        buildFile.delete()

        projectDir.deleteDir()
        projectDir.mkdir()

        if(tag) {
            gitTagCheckOut(projectDir, System.properties['giturl'].toString(), branch, tag)
        } else {
            gitCheckOut(projectDir, System.properties['giturl'].toString(), branch)
        }

        canonicalName = testName.getMethodName().replaceAll(' ', '-')

        Project p = ProjectBuilder.builder().withName(canonicalName).withProjectDir(testProjectDir).build()

        Plugin plugin = new ScmVersionPlugin()
        plugin.apply(p)

        ScmExtension scmConfig = p.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)
        scmConfig.user.name = System.properties['gituser'].toString()
        scmConfig.user.password = System.properties['gitpasswd'].toString()
        scmConfig.prefixes.tagPrefix = 'CLRELEASE'

        return p
    }
}
