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
package com.intershop.gradle.scm

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.utils.ScmType
import com.intershop.gradle.test.AbstractProjectSpec
import org.gradle.api.Plugin
import org.gradle.api.Task

class PluginSpec extends AbstractProjectSpec {

    @Override
    Plugin getPlugin() {
        return new ScmVersionPlugin()
    }

    def 'should add extension named scmConfig and changeLog'() {
        when:
        plugin.apply(project)
        Task task = project.tasks.findByName(ScmVersionPlugin.CHANGELOG_TASK)

        then:
        project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)
        ! ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version.disableSCM
        ((ScmExtension)project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)).version.patternDigits == 2

        task.outputs.files.contains(new File(project.getBuildDir(), 'changelog/changelog.asciidoc'))
    }

    def 'should add tasks for version handling'() {
        when:
        plugin.apply(project)

        then:
        project.tasks.findByName(ScmVersionPlugin.SHOW_VERSION_TASK)
        project.tasks.findByName(ScmVersionPlugin.TO_VERSION_TASK)
        project.tasks.findByName(ScmVersionPlugin.RELEASE_TASK)
        project.tasks.findByName(ScmVersionPlugin.CREATE_BRANCH_TASK)
        project.tasks.findByName(ScmVersionPlugin.CREATE_TAG_TASK)

        project.hasProperty('useSCMVersionConfig')
        project.getProperties().get('useSCMVersionConfig').toString().toLowerCase() == 'true'
    }

    def 'extension contains correct SCM type - git'() {
        setup:
        File gitDir = new File(testProjectDir, '.git')
        gitDir.mkdirs()

        when:
        plugin.apply(project)
        ScmExtension config = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)

        then:
        config != null
        config.scmType == ScmType.git
    }

    def 'extension contains correct SCM type - file'() {
        when:
        plugin.apply(project)
        ScmExtension config = project.extensions.getByName(ScmVersionPlugin.SCM_EXTENSION)

        then:
        config != null
        config.scmType == ScmType.file
        config.prefixes.featurePrefix == 'FB'
    }

}
