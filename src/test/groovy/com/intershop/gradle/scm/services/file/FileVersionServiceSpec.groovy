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
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class FileVersionServiceSpec extends Specification {

    @TestDir
    File projectDir

    @Rule
    TestName testName = new TestName()

    protected String canonicalName
    protected Project project

    def 'getVersionObject for file system - three digits'() {
        setup:
        project = prepareProject()

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        FileVersionService client = versionConfig.versionService

        then:
        client.versionObject.changed
        client.versionObject.version.toString() == '1.0.0'
    }

    def 'getVersionObject for file system - four digits'() {
        setup:
        project = prepareProject()

        when:
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version
        versionConfig.type = 'fourDigits'
        FileVersionService client = versionConfig.versionService

        then:
        client.versionObject.changed
        client.versionObject.version.toString() == '1.0.0.0'
    }

    private Project prepareProject() {
        def testFile = new File(projectDir, 'test.properties')
        testFile.text = '''com.test.property = 1
        com.test.value = 1'''

        canonicalName = testName.getMethodName().replaceAll(' ', '-')

        Project p = ProjectBuilder.builder().withName(canonicalName).withProjectDir(projectDir).build()
        p.pluginManager.apply(ScmVersionPlugin.class)

        return p
    }
}
