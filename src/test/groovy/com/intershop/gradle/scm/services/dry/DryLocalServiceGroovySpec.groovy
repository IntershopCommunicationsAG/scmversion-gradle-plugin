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
import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.services.git.GitLocalService
import com.intershop.gradle.scm.test.utils.AbstractScmGroovySpec
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import spock.lang.Requires
import spock.lang.Unroll

@Unroll
class DryLocalServiceGroovySpec extends AbstractScmGroovySpec {

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

        when:
        gitCheckOut(projectDir, System.properties['giturl'].toString(), 'master' )
        Project project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(projectDir).build()

        then:
        (new File(projectDir, ".git")).exists()

        when:
        project.pluginManager.apply(ScmVersionPlugin.class)

        then:
        (new File(projectDir, ".git")).exists()

        ScmExtension scmConfig = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)

        VersionExtension versionExt = scmConfig.version
        DryLocalService localService = new DryLocalService(projectDir, scmConfig.prefixes, scmConfig.localService)

        then:
        versionExt != null
        localService.remoteUrl == System.properties['giturl']
        localService.branchName == 'master'
        ! localService.changed
    }
}
