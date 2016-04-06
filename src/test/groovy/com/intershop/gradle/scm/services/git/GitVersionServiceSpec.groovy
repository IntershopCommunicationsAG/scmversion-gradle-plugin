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
import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.test.utils.AbstractScmSpec
import com.intershop.gradle.test.util.TestDir
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Requires
import spock.lang.Unroll

@Requires({ System.properties['giturl'] &&
        System.properties['gituser'] &&
        System.properties['gitpasswd'] })
@Unroll
class GitVersionServiceSpec extends AbstractScmSpec {

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
        project = prepareProject('master', 'SBRELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        GitVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '2.0.0'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'getVersionObject for master from tag with remote changes'() {
        setup:
        project = prepareProject('SB_1.1', 'SBRELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        GitVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '1.1.0'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'getVersionObject for branch from tag'() {
        setup:
        project = prepareProject('SB_2.0', 'SBRELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        GitVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        ! client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '2.0.0'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'getVersionObject for branch from tag with local changes'() {
        setup:
        project = prepareProject('SB_2.0', 'SBRELEASE', 'FB', 'SB')
        gitChangeTestFile(projectDir)

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        GitVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '2.0.0'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'getVersionObject for branch from tag with remote changes'() {
        setup:
        project = prepareProject('SB_1.1', 'SBRELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        GitVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '1.1.0'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'getVersionObject for feature branch from tag with changes'() {
        setup:
        project = prepareProject('FB_1.1.0-test', 'SBRELEASE', 'FB', 'SB')

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        GitVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)

        then:
        client.getVersionObject().isChanged()
        client.getVersionObject().version.toString() == '1.1.0-test'
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def "create simple tag"() {
        setup:
        project = prepareProject('master', 'SBRELEASE', 'FB', 'SB')
        String version = "4.1.1"

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        GitVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)
        def rev = client.createTag(version)

        then:
        rev != null

        cleanup:
        gitTagRemove(projectDir, "SBRELEASE_${version}")
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def "create simple branch"() {
        setup:
        project = prepareProject('master', 'SBRELEASE', 'FB', 'SB')
        String version = "4.0.0"

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        GitVersionService client = ScmBuilder.getScmVersionService(project, versionConfig)
        def rev = client.createBranch(version, false)

        then:
        rev != null

        cleanup:
        gitBranchRemove(projectDir, "SB_${version}")
    }

    private Project prepareProject(String branch, String tagPrefix, String featurePrefix, String stabPrefix, String tag = '') {
        File buildFile = new File(projectDir, 'build.gradle')
        buildFile.delete()

        if (tag) {
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

        scmConfig.prefixes.tagPrefix = tagPrefix
        scmConfig.prefixes.featurePrefix = featurePrefix
        scmConfig.prefixes.stabilizationPrefix = stabPrefix

        return p
    }

}
