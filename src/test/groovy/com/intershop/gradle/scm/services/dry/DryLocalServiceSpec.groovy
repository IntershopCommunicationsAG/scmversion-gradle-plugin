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


package com.intershop.gradle.scm.services.dry

import com.intershop.gradle.scm.ScmVersionPlugin
import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.services.git.GitLocalService
import com.intershop.gradle.scm.test.utils.AbstractScmSpec
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import spock.lang.Requires
import spock.lang.Unroll

@Unroll
class DryLocalServiceSpec extends AbstractScmSpec {

    @Rule TemporaryFolder temp

    @Rule
    TestName testName = new TestName()

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'calculate local information of new project'() {
        setup:
        String canonicalName = testName.getMethodName().replaceAll(' ', '-')

        def projectDir = temp.newFolder()
        Project project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(testProjectDir).build()
        Plugin plugin = new ScmVersionPlugin()
        plugin.apply(project)

        ScmExtension scmConfig = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)
        gitCheckOut(projectDir, System.properties['giturl'].toString(), 'master' )

        when:
        GitLocalService gitLocalService = new GitLocalService(projectDir, scmConfig)
        DryLocalService localService = new DryLocalService(projectDir, scmConfig, gitLocalService)

        then:
        localService.getRemoteUrl() == System.properties['giturl']
        localService.getBranchName() == 'master'
        ! localService.isChanged()
    }
}
