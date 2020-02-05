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
import com.intershop.gradle.scm.test.utils.AbstractScmGroovySpec
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.test.util.TestDir
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Requires
import spock.lang.Unroll

@Unroll
class DryVersionServiceGroovySpec extends AbstractScmGroovySpec {

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    protected String canonicalName
    protected Project project

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'getVersionObject for master from branches'() {
        setup:
        project = prepareProject('git', 'master', 'SBRELEASE', 'FB', 'SB')

        when:
        ScmExtension scmConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION))
        DryLocalService localService = new DryLocalService(projectDir, scmConfig.prefixes, scmConfig.localService)
        DryVersionService client = new DryVersionService(scmConfig.version, localService, scmConfig.version.versionService)

        then:
        client.versionObject.changed
        client.versionObject.version.toString() == '2.0.0'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'getVersionObject for branch from tag with changes'() {
        setup:
        project = prepareProject('git', 'SB_1.1', 'SBRELEASE', 'FB', 'SB')
        gitChangeTestFile(projectDir)

        when:
        ScmExtension scmConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION))
        DryLocalService localService = new DryLocalService(projectDir, scmConfig.prefixes, scmConfig.localService)
        DryVersionService client = new DryVersionService(scmConfig.version, localService, scmConfig.version.versionService)

        then:
        client.versionObject.changed == true
        client.versionObject.version.toString() == '1.1.0'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'create simple branch from git'() {
        setup:
        project = prepareProject('git', 'master', 'SBRELEASE', 'FB', 'SB')
        String version = "4.0.0.0"

        when:
        ScmExtension scmConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION))
        DryLocalService localService = new DryLocalService(projectDir, scmConfig.prefixes, scmConfig.localService)
        DryVersionService client = new DryVersionService(scmConfig.version, localService, scmConfig.version.versionService)

        def rev = client.createBranch(version, false, null)

        then:
        rev == ''
    }

    private Project prepareProject(String type, String branch, String tagPrefix, String featurePrefix, String stabPrefix, String tag = '') {
        File buildFile = new File(projectDir, 'build.gradle')
        buildFile.delete()

        projectDir.deleteDir()
        projectDir.mkdir()

        if (tag) {
            gitTagCheckOut(projectDir, System.properties['giturl'].toString(), branch, tag)
        } else {
            gitCheckOut(projectDir, System.properties['giturl'].toString(), branch)
        }

        canonicalName = testName.getMethodName().replaceAll(' ', '-')

        Project p = ProjectBuilder.builder().withName(canonicalName).withProjectDir(testProjectDir).build()
        p.pluginManager.apply(ScmVersionPlugin.class)

        ScmExtension scmConfig = p.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)
        scmConfig.user.name = System.properties['gituser'].toString()
        scmConfig.user.password = System.properties['gitpasswd'].toString()

        scmConfig.prefixes.tagPrefix = tagPrefix
        scmConfig.prefixes.featurePrefix = featurePrefix
        scmConfig.prefixes.stabilizationPrefix = stabPrefix

        return p
    }
}
