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
import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.test.utils.AbstractScmSpec
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
class SvnLocalServiceSpec extends AbstractScmSpec {

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'calculate local information of svn trunk project'() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'trunk')

        when:
        SvnLocalService infoService = new SvnLocalService(projectDir, scmConfig)

        then:
        infoService.getProjectDir().equals(projectDir)
        infoService.getRemoteUrl() == "${System.properties['svnurl']}/trunk".toString()
        infoService.getProjectRootSvnUrl().toString() == System.properties['svnurl']
        infoService.getRevision() > 0
        infoService.getBranchType() == BranchType.trunk
        infoService.getBranchName() == 'trunk'
        infoService.getFeatureBranchName() == ''
        ! infoService.changed
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'calculate local information of svn branch project'() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'branches/SB_1.1')

        when:
        SvnLocalService infoService = new SvnLocalService(projectDir, scmConfig)

        then:
        infoService.getProjectDir().equals(projectDir)
        infoService.getRemoteUrl() == "${System.properties['svnurl']}/branches/SB_1.1".toString()
        infoService.getProjectRootSvnUrl().toString() == System.properties['svnurl']
        infoService.getRevision() > 0
        infoService.getBranchType() == BranchType.branch
        infoService.getBranchName() == 'SB_1.1'
        infoService.getFeatureBranchName() == ''
        ! infoService.changed
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'calculate local information of svn branch project - changed'() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'branches/SB_1.1')
        svnChangeTestFile(projectDir)

        when:
        SvnLocalService infoService = new SvnLocalService(projectDir, scmConfig)

        then:
        infoService.getProjectDir().equals(projectDir)
        infoService.getRemoteUrl() == "${System.properties['svnurl']}/branches/SB_1.1".toString()
        infoService.getProjectRootSvnUrl().toString() == System.properties['svnurl']
        infoService.getRevision() > 0
        infoService.getBranchType() == BranchType.branch
        infoService.getBranchName() == 'SB_1.1'
        infoService.getFeatureBranchName() == ''
        infoService.changed
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'calculate local information of svn feature branch project'() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'branches/FB_1.0.0-fb-123')

        when:
        SvnLocalService infoService = new SvnLocalService(projectDir, scmConfig)

        then:
        infoService.getProjectDir().equals(projectDir)
        infoService.getRemoteUrl() == "${System.properties['svnurl']}/branches/FB_1.0.0-fb-123".toString()
        infoService.getProjectRootSvnUrl().toString() == System.properties['svnurl']
        infoService.getRevision() > 0
        infoService.getBranchType() == BranchType.featureBranch
        infoService.getBranchName() == 'FB_1.0.0-fb-123'
        infoService.getFeatureBranchName() == 'fb-123'
        ! infoService.changed
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'calculate local information of svn tag project'() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'tags/SBRELEASE_1.1.0')
        scmConfig.prefixes {
            stabilizationPrefix = 'SB'
            featurePrefix = 'FB'
            tagPrefix = 'SBRELEASE'
        }

        when:
        SvnLocalService infoService = new SvnLocalService(projectDir, scmConfig)

        then:
        infoService.getProjectDir().equals(projectDir)
        infoService.getRemoteUrl() == "${System.properties['svnurl']}/tags/SBRELEASE_1.1.0".toString()
        infoService.getProjectRootSvnUrl().toString() == System.properties['svnurl']
        infoService.getRevision() > 0
        infoService.getBranchType() == BranchType.tag
        infoService.getBranchName() == 'SBRELEASE_1.1.0'
        infoService.getFeatureBranchName() == ''
        ! infoService.changed
    }

    ScmExtension prepareProject(File projectDir, String path) {
        String canonicalName = testName.getMethodName().replaceAll(' ', '-')
        svnCheckOut(projectDir, "${System.properties['svnurl']}/${path}")

        Project project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(projectDir).build()
        Plugin plugin = new ScmVersionPlugin()
        plugin.apply(project)

        new File(projectDir, 'build.gradle').delete()

        ScmExtension scmConfig = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)

        return scmConfig
    }
}
