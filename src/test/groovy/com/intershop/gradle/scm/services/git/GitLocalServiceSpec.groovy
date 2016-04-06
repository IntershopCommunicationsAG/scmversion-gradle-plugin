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
import com.intershop.gradle.scm.extension.ScmExtension
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
class GitLocalServiceSpec extends AbstractScmSpec {

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'calculate local information of master branch'() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'master')

        when:
        GitLocalService infoService = new GitLocalService(projectDir, scmConfig)

        then:
        infoService.getRemoteUrl() == System.properties['giturl']
        infoService.getBranchName() == 'master'
        ! infoService.isChanged()
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def "calculate local information of branch project"() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'SB_1.1')

        when:
        GitLocalService infoService = new GitLocalService(projectDir, scmConfig)

        then:
        infoService.getRemoteUrl() == System.properties['giturl']
        infoService.getBranchName() == 'SB_1.1'
        ! infoService.isChanged()
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def "calculate local information of master branch - changed"() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'SB_1.1')
        gitChangeTestFile(projectDir)

        when:
        GitLocalService infoService = new GitLocalService(projectDir, scmConfig)

        then:
        infoService.getRemoteUrl() == System.properties['giturl']
        infoService.getBranchName() == 'SB_1.1'
        infoService.isChanged()
    }

    ScmExtension prepareProject(File projectDir, String path) {
        String canonicalName = testName.getMethodName().replaceAll(' ', '-')

        // prepare git checkout
        if(! projectDir.deleteDir()) {
            log.error('It was not possible to delete {}', projectDir)
        }

        gitCheckOut(projectDir, System.properties['giturl'].toString(), path )

        Project project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(projectDir).build()
        Plugin plugin = new ScmVersionPlugin()
        plugin.apply(project)

        ScmExtension scmConfig = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)

        return scmConfig
    }
}
