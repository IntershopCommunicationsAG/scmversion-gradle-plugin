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


package com.intershop.gradle.scm.services.file

import com.intershop.gradle.scm.ScmVersionPlugin
import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.test.util.TestDir
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification
import spock.lang.Unroll

@Slf4j
@Unroll
class FileLocalServiceSpec extends Specification {

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    def 'calculate local information of new project'() {
        setup:
        ScmExtension scmExtension = prepareProject(projectDir)

        def testFile = new File(projectDir, 'test.properties')
        testFile.text = '''com.test.property = 1
        com.test.value = 1'''

        when:
        FileLocalService infoService = scmExtension.localService

        then:
        infoService.getRemoteUrl() == projectDir.toURI().toURL().toString()
        infoService.getBranchName() == 'trunk'
        infoService.changed
    }

    ScmExtension prepareProject(File projectDir) {
        String canonicalName = testName.getMethodName().replaceAll(' ', '-')

        Project project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(projectDir).build()
        project.pluginManager.apply(ScmVersionPlugin.class)

        ScmExtension scmConfig = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)

        return scmConfig
    }
}
