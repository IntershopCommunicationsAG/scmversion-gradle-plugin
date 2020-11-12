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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName

import com.intershop.gradle.scm.ScmVersionPlugin
import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.test.utils.AbstractScmSpec
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.ScmBranchFilter
import com.intershop.gradle.test.util.TestDir

import spock.lang.Requires
import spock.lang.Unroll

@Requires({ System.properties['giturl'] &&
        System.properties['gituser'] &&
        System.properties['gitpasswd'] })
@Unroll
class GitRemoteServiceSpec extends AbstractScmSpec {

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'get tag map'() {
        setup:
        ScmExtension scmConfig = prepareProject(projectDir, 'master')

        when:
        GitRemoteService srs = new GitRemoteService(new GitLocalService(projectDir, scmConfig),
                new ScmUser(System.properties['gituser'].toString(), System.properties['gitpasswd'].toString()))

        Map<String, BranchObject> map = srs.getTagMap(
                new ScmBranchFilter() {
                    @Override
                    String getVersionStr(String branch) {
                        def versionGroup = (branch =~ /(\d+\.?\d+\.?\d+\.?\d*-?([A-za-z]+\.?\d+$)?)/)
                        if(versionGroup.size() > 0) {
                            return (versionGroup[0][1])
                        }
                    }
                })

        then:
        map.size() > 0
    }

    ScmExtension prepareProject(File projectDir, String path) {
        String canonicalName = testName.getMethodName().replaceAll(' ', '-')

        // prepare git checkout
        if(! projectDir.deleteDir()) {
            log.error('It was not possible to delete {}', projectDir)
        }

        gitCheckOut(projectDir, System.properties['giturl'].toString(), path)

        Project project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(projectDir).build()
        Plugin plugin = new ScmVersionPlugin()
        plugin.apply(project)

        ScmExtension scmConfig = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)
        return scmConfig
    }
}
