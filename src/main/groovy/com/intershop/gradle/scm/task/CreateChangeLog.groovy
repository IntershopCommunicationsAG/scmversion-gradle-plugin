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
package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.builder.ScmBuilder
import com.intershop.gradle.scm.services.ScmChangeLogService
import com.intershop.gradle.scm.utils.ScmType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * <p>Gradle task 'changeLog'</p>
 * <p>It creates a asciidoc file with all changes between
 * the current version and tag of the previous version.</p>
 * <p>The version must be assoziated with tags. Parts of the
 * necessary configuration are shared with the scmversion plugin.</p>
 */
@CompileStatic
@Slf4j
class CreateChangeLog extends DefaultTask {

    final PropertyState<File> changelogFile = project.property(File)
    final PropertyState<String> targetVersion = project.property(String)
    final PropertyState<Boolean> filterProject = project.property(Boolean)

    @OutputFile
    File getChangelogFile() {
        changelogFile.get()
    }

    void setChangelogFile(File changelogFile) {
        this.changelogFile.set(changelogFile)
    }

    void setChangelogFile(Provider<File> changelogFile) {
        this.changelogFile.set(changelogFile)
    }

    @Input
    String getTargetVersion() {
        targetVersion.get()
    }

    void setTargetVersion(String targetVersion) {
        this.targetVersion.set(targetVersion)
    }

    void setTargetVersion(Provider<String> targetVersion) {
        this.targetVersion.set(targetVersion)
    }

    @Input
    Boolean getFilterProject() {
        filterProject.get()
    }

    void setFilterProject(Boolean filterProject) {
        this.filterProject.set(filterProject)
    }

    void setFilterProject(Provider<Boolean> filterProject) {
        this.filterProject.set(filterProject)
    }

    CreateChangeLog() {
        this.outputs.upToDateWhen { false }
        this.setDescription('Creates a changelog based on SCM information in ASCIIDoc format')
    }

    @TaskAction
    void createLog() {
        ScmChangeLogService changeLogService = ScmBuilder.getScmChangeLogService(this.getProject())

        if(changeLogService.type != ScmType.file) {
            // set configuration parameter
            changeLogService.setTargetVersion(getTargetVersion())
            changeLogService.changelogFile = getChangelogFile()
            changeLogService.filterProject = getFilterProject().booleanValue()

            changeLogService.changelogFile.getParentFile().mkdirs()
            if (changeLogService.changelogFile.exists()) {
                changeLogService.changelogFile.delete()
            }
            changeLogService.changelogFile.createNewFile()

            changeLogService.createLog()
            log.info('Change log was written to {}', changeLogService.changelogFile.absolutePath)
        } else {
            log.warn('The used scm does not support the creation of a change log.')
        }
    }
}
