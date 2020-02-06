/*
 * Copyright 2019 Intershop Communications AG.
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

import com.intershop.gradle.scm.services.ScmChangeLogService
import com.intershop.gradle.scm.services.file.FileChangeLogService
import com.intershop.gradle.scm.services.git.GitChangeLogService
import com.intershop.gradle.scm.services.git.GitLocalService
import com.intershop.gradle.scm.services.git.GitRemoteService
import com.intershop.gradle.scm.utils.ScmType
import com.intershop.gradle.scm.utils.getValue
import com.intershop.gradle.scm.utils.setValue
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

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
abstract class ChangeLogExtension @Inject constructor(private val scmExtension: ScmExtension) {

    companion object {
        /**
         * Default path for change log file.
         */
        const val CHANGELOGFILE_PATH = "changelog/changelog.asciidoc"
    }

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    /**
     * Inject service of ProjectLayout (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val projectLayout: ProjectLayout

    /**
     * This provides the functionality for all
     * tasks etc.
     *
     * @property changelogService
     */
    val changelogService: ScmChangeLogService by lazy {
        val tempChangelogService = if (scmExtension.scmType == ScmType.GIT) {
            with(scmExtension) {
                GitChangeLogService(version, GitRemoteService(localService as GitLocalService, user, key))
            }
        } else {
            FileChangeLogService()
        }
        tempChangelogService
    }

    private val targetVersioneProperty: Property<String> = objectFactory.property(String::class.java)
    private val changelogFileProperty: RegularFileProperty = objectFactory.fileProperty()

    init {
        changelogFileProperty.convention(projectLayout.buildDirectory.file(CHANGELOGFILE_PATH))
    }

    /**
     * This is provider for the target version.
     *
     * @property targetVersioneProvider
     */
    val targetVersioneProvider: Provider<String>
        get() = targetVersioneProperty

    /**
     * This is the target version for the
     * calculation of the change log.
     *
     * @property targetVersion
     */
    var targetVersion: String by targetVersioneProperty

    /**
     * This is provider for the changelog file.
     *
     * @property changelogFileProvider
     */
    val changelogFileProvider: Provider<RegularFile> = changelogFileProperty

    /**
     * This is the property for the
     * change log file.
     *
     * @property changelogFile
     */
    var changelogFile: File
        get() = changelogFileProperty.get().asFile
        set(value)  = changelogFileProperty.set(value)
}
