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

@Unroll
class SvnRemoteServiceSpec extends AbstractScmSpec {

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'get tag map'() {
        setup:
        Project project = prepareProject(projectDir)
        svnCheckOut(projectDir, "${System.properties['svnurl']}/trunk")
        ScmExtension scmConfig = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)

        when:
        SvnRemoteService srs = new SvnRemoteService(new SvnLocalService(projectDir, scmConfig),
                new ScmUser(System.properties['svnuser'].toString(), System.properties['svnpasswd'].toString()))

        Map<String, BranchObject> map = srs.getTagMap(
                new ScmBranchFilter() {
                    @Override
                    String getVersionStr(String branch) {
                        def versionGroup = (branch =~ /(\d+\.?\d+\.?\d+\.?\d*-?([A-za-z]+\.?\d+$)?)/)
                        if(versionGroup.size() > 0) {
                            return (versionGroup[0][1])
                        }
                    }
                }
        )

        then:
        map.size() > 0
    }

    Project prepareProject(File projectDir) {
        String canonicalName = testName.getMethodName().replaceAll(' ', '-')

        Project project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(projectDir).build()
        Plugin plugin = new ScmVersionPlugin()
        plugin.apply(project)

        return project
    }

}
