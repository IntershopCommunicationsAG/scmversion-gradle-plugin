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
package com.intershop.gradle.scm.services.svn

import com.intershop.gradle.scm.ScmVersionPlugin
import com.intershop.gradle.scm.builder.ScmBuilder
import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.services.ScmChangeLogService
import com.intershop.gradle.scm.test.utils.AbstractScmSpec
import com.intershop.gradle.test.util.TestDir
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Requires
import spock.lang.Unroll

@Unroll
class SvnChangeLogSpec extends AbstractScmSpec{

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    protected String canonicalName
    protected Project project

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'get change log on svn between trunk and tag'() {
        setup:
        project = prepareProject('trunk')
        File targetDir = new File(projectDir, 'build/changelog')
        targetDir.mkdirs()
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog

        when:
        scls.createLog()

        then:
        testLog.exists()
        testLog.text.contains('add change on trunk')
        testLog.text.contains('| M | /trunk/test.properties')
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'get change log on svn between tag and not configured previous tag'() {
        setup:
        project = prepareProject('tags/CLRELEASE_2.0.0')
        File targetDir = new File(projectDir, 'build/changelog')
        targetDir.mkdirs()
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog

        when:
        scls.createLog()

        then:
        testLog.exists()
        testLog.text.contains('This list contains changes since version 1.5.0.')
        testLog.text.contains('JIRA-2345: change for additional test')
        testLog.text.contains('| M | /trunk/test.properties')
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'get change log on svn between tag and configured tag'() {
        setup:
        project = prepareProject('tags/CLRELEASE_2.0.0')
        File targetDir = new File(projectDir, 'build/changelog')
        targetDir.mkdirs()
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog
        scls.targetVersion = '1.0.0'

        when:
        scls.createLog()

        then:
        testLog.exists()
        testLog.text.contains('This list contains changes since version 1.0.0.')
        testLog.text.contains('JIRA-2345: change for additional test')
        testLog.text.contains('| M | /trunk/test.properties')
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'configured previous version does not exists'() {
        setup:
        project = prepareProject('tags/CLRELEASE_2.0.0')
        File targetDir = new File(projectDir, 'build/changelog')
        targetDir.mkdirs()
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog
        scls.targetVersion = '0.9.0'

        when:
        scls.createLog()

        then:
        def e = thrown(GradleException)
        e.message == 'The configured previous version is not available! Please check your configuration.'
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'there is no previous version tag'() {
        setup:
        project = prepareProject('tags/CLRELEASE_1.0.0')
        File targetDir = new File(projectDir, 'build/changelog')
        targetDir.mkdirs()
        File testLog = new File(targetDir, 'log.asciidoc')

        ScmChangeLogService scls = ScmBuilder.getScmChangeLogService(project)
        scls.changelogFile = testLog

        when:
        scls.createLog()

        then:
        def e = thrown(GradleException)
        e.message == 'There is no previous version! Please check your configuration.'
    }

    private Project prepareProject(String path) {
        svnCheckOut(projectDir, "${System.properties['svnurl']}/${path}")
        canonicalName = testName.getMethodName().replaceAll(' ', '-')

        Project p = ProjectBuilder.builder().withName(canonicalName).withProjectDir(testProjectDir).build()

        Plugin plugin = new ScmVersionPlugin()
        plugin.apply(p)

        buildFile.delete()

        ScmExtension scmConfig = p.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)
        scmConfig.user.name = System.properties['svnuser'].toString()
        scmConfig.user.password = System.properties['svnpasswd'].toString()
        scmConfig.prefixes.tagPrefix = 'CLRELEASE'

        return p
    }
}
