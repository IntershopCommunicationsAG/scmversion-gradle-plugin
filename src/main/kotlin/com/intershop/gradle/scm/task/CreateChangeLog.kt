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
package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.utils.ScmType
import com.intershop.gradle.scm.utils.getValue
import com.intershop.gradle.scm.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * This is the implementation of Gradle
 * task to create a change log from the
 * previous tag (target) and the current
 * tag.
 */
abstract class CreateChangeLog: DefaultTask() {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val changelogFileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val previousVersionProperty: Property<String> = project.objects.property(String::class.java)

    init {
        description = "Creates a changelog based on SCM information in ASCIIDoc format"
        group = "Release Version Plugin"

        previousVersionProperty.convention("")
    }

    /**
     * This is the provider for the targetVersion.
     */
    fun providePreviousVersion(previousVersion: Provider<String>) = previousVersionProperty.set(previousVersion)

    /**
     * This is the previousVersion.
     *
     * @property previousVersion
     */
    @get:Optional
    @get:Input
    var previousVersion by previousVersionProperty

    /**
     * Provider for output file property.
     */
    fun provideChangelogFile(changelogFile: Provider<RegularFile>) {
        changelogFileProperty.set(changelogFile)
    }

    /**
     * Input file property for output.
     *
     * @property changelogFile
     */
    @get:OutputFile
    var changelogFile: File
        get() = changelogFileProperty.get().asFile
        set(value) = changelogFileProperty.set(value)

    /**
     * Implementation of the task action.
     */
    @TaskAction
    fun createLog() {
        val scmConfig: ScmExtension = project.extensions.getByType(ScmExtension::class.java)
        val changelogService = scmConfig.changelog.changelogService

        if(scmConfig.scmType != ScmType.FILE) {
            // set configuration parameter
            changelogFile.parentFile.mkdirs()
            if (changelogFile.exists()) {
                changelogFile.delete()
            }
            changelogFile.createNewFile()

            val tempPrevVersion = if(previousVersion.isNotEmpty()) { previousVersion } else { null }
            changelogService.createLog(changelogFile, tempPrevVersion)
            project.logger.info("Change log was written to {}", changelogFile.absolutePath)
        } else {
            project.logger.warn("The used scm does not support the creation of a change log.")
        }
    }
}
