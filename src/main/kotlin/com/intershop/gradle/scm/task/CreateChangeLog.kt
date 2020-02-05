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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class CreateChangeLog: DefaultTask() {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val changelogFileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val targetVersionProperty: Property<String> = project.getObjects().property(String::class.java)

    init {
        description = "Creates a changelog based on SCM information in ASCIIDoc format"
        group = "Release Version Plugin"
    }

    /**
     * This is the provider for the targetVersion.
     */
    fun provideTargetVersion(targetVersion: Provider<String>) = targetVersionProperty.set(targetVersion)

    /**
     * This is the targetVersion.
     *
     * @property targetVersion
     */
    @get:Input
    var targetVersion by targetVersionProperty

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

    @TaskAction
    fun createLog() {
        val scmConfig: ScmExtension = project.extensions.getByType(ScmExtension::class.java)
        val changelogService = scmConfig.changelog.changelogService

        if(scmConfig.scmType != ScmType.file) {
            // set configuration parameter
            changelogFile.getParentFile().mkdirs()
            if (changelogFile.exists()) {
                changelogFile.delete()
            }
            changelogFile.createNewFile()

            changelogService.createLog(changelogFile, targetVersion)
            project.logger.info("Change log was written to {}", changelogFile.absolutePath)
        } else {
            project.logger.warn("The used scm does not support the creation of a change log.")
        }
    }
}