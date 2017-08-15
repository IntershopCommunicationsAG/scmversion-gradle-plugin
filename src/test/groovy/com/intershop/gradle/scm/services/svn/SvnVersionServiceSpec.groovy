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
import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.test.utils.AbstractScmSpec
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.test.util.TestDir
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Requires
import spock.lang.Unroll

@Unroll
@Slf4j
class SvnVersionServiceSpec extends AbstractScmSpec {

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    protected String canonicalName
    protected Project project

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'getVersionObject for trunk from branches'() {
        setup:
        project = prepareProject('trunk', 'RELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        versionConfig.versionBranch = BranchType.branch.toString()
        SvnVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '2.0.0'
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'getVersionObject for trunk from branches with local changes'() {
        setup:
        project = prepareProject('trunk', 'RELEASE', 'FB', 'SB')
        svnChangeTestFile(projectDir)

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        versionConfig.versionBranch = BranchType.branch.toString()
        SvnVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '2.0.0'
        versionConfig.getBranchName() == 'trunk'
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'getVersionObject for branch from tags withou remote changes'() {
        setup:
        project = prepareProject('branches/SB_1.1', 'SBRELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        SvnVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '1.1.0'
        versionConfig.getBranchName() == 'SB_1.1'
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'getVersionObject for branch from tags with remote changes'() {
        setup:
        project = prepareProject('branches/SB_2.0', 'SBRELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        SvnVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '2.0.0'
        versionConfig.getBranchName() == 'SB_2.0'
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'getVersionObject for feature branch'() {
        setup:
        project = prepareProject('branches/FB_1.0.0-fb-123', 'SBRELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        SvnVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '1.0.0-fb-123'
        versionConfig.getBranchName() == 'FB_1.0.0-fb-123'
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'create branch from trunk'() {
        setup:
        project = prepareProject('trunk', 'SBRELEASE', 'FB', 'SB')
        String version = "4.0.0"

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        versionConfig.versionBranch = BranchType.branch.toString()
        SvnVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)
        String r = client.createBranch(version, false)

        then:
        Long.parseLong(r) > 0

        cleanup:
        svnRemove("${System.properties['svnurl']}/branches/SB_${version}")
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'create tag from trunk'() {
        setup:
        project = prepareProject('trunk', 'SBRELEASE', 'FB', 'SB')
        String version = '4.1.1'

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        versionConfig.versionBranch = BranchType.branch.toString()
        SvnVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)
        String r = client.createTag(version)

        then:
        Long.parseLong(r) > 0

        cleanup:
        svnRemove("${System.properties['svnurl']}/tags/SBRELEASE_${version}")
    }

    private Project prepareProject(String branch, String tagPrefix, String featurePrefix, String stabPrefix) {
        File buildFile = new File(projectDir, 'build.gradle')
        buildFile.delete()

        svnCheckOut(projectDir, "${System.properties['svnurl']}/${branch}")

        canonicalName = testName.getMethodName().replaceAll(' ', '-')

        Project p = ProjectBuilder.builder().withName(canonicalName).withProjectDir(testProjectDir).build()
        Plugin plugin = new ScmVersionPlugin()
        plugin.apply(p)

        ScmExtension scmConfig = p.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)
        scmConfig.user.name = System.properties['svnuser'].toString()
        scmConfig.user.password = System.properties['svnpasswd'].toString()

        scmConfig.prefixes.tagPrefix = tagPrefix
        scmConfig.prefixes.featurePrefix = featurePrefix
        scmConfig.prefixes.stabilizationPrefix = stabPrefix

        return p
    }
}
