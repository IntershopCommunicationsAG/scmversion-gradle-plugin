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
abstract class ChangeLogExtension @Inject constructor() {

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

    private val previousVersionProperty: Property<String> = objectFactory.property(String::class.java)
    private val changelogFileProperty: RegularFileProperty = objectFactory.fileProperty()

    init {
        changelogFileProperty.convention(projectLayout.buildDirectory.file(CHANGELOGFILE_PATH))
        previousVersionProperty.convention("")
    }

    /**
     * This is provider for the previous version.
     *
     * @property previousVersionProperty
     */
    val previousVersionProvider: Provider<String>
        get() = previousVersionProperty

    /**
     * This is the previous version for the
     * calculation of the change log.
     *
     * @property previousVersion
     */
    var previousVersion: String by previousVersionProperty

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
