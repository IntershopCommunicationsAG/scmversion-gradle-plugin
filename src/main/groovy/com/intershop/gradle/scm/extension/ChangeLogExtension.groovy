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

package com.intershop.gradle.scm.extension

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider

/**
 * <p>This is the extension object for the Intershop version plugin.</p>
 *
 * <pre>
 * {@code
 * changeLog {
 *    defaultTargetVersion = '1.0.0'
 * }
 * </pre>
 */

@CompileStatic
@Slf4j
class ChangeLogExtension extends AbstractExtension {

    final PropertyState<String> targetVersion

    Provider<String> getTargetVersionProvider() {
        targetVersion
    }

    String getTargetVersion() {
        targetVersion.get()
    }

    void setTargetVersion(String targetVersion) {
        this.targetVersion.set(targetVersion)
    }

    final PropertyState<File> changelogFile

    Provider<File> getChangelogFileProvider() {
        changelogFile
    }

    File getChangelogFile() {
        changelogFile.get()
    }

    void setChangelogFile(File changelogFile) {
        this.changelogFile.set(changelogFile)
    }

    final PropertyState<Boolean> filterProject

    Provider<Boolean> getFilterProjectProvider() {
        filterProject
    }

    Boolean getFilterProjec() {
        filterProject.get()
    }

    void setFilterProjec(boolean filterProject) {
        this.filterProject.set(new Boolean(filterProject))
    }

    /**
     * Initialize this extension and set default values
     * If environment values are set for some keys this
     * values will be used.
     *
     * @param project
     */
    ChangeLogExtension(Project project) {
        super(project)

        targetVersion = project.property(String)
        changelogFile = project.property(File)
        filterProject = project.property(Boolean)
    }



}
