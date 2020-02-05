package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.ScmException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class CreateTag: DefaultTask() {

    init {
        outputs.upToDateWhen { false }

        description = "Creates an SCM tag With a specific version from the working copy"
        group = "Release Version Plugin"
    }

    @Throws(GradleException::class)
    @TaskAction
    fun tag() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version
        val versionService = versionConfig.versionService

        if(versionService.localService.branchType == BranchType.tag) {
            project.logger.error("It is not possible to create a tag from an existing tag! Please check your working copy.")
            throw GradleException("It is not possible to create a tag from an existing tag! Please check your working copy.")
        }

        // create tag
        var version = versionService.preVersion

        try {
            versionService.createTag(version.toString(), null)
        } catch ( ex: Exception ) {
            throw GradleException("It is not possible to create a tag on the SCM! (${ex.message})")
        }

        println("""
            |----------------------------------------------
            |        Tag created: ${version}
            |----------------------------------------------""".trimMargin())
    }
}