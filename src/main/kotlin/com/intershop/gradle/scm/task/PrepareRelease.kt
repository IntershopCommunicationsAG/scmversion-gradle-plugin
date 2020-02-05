package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.utils.BranchType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class PrepareRelease: DefaultTask() {

    init {
        outputs.upToDateWhen { false }

        description = "Prepare a release process, create branch and/or tag, moves the working copy to the tag"
        group = "Release Version Plugin"
    }

    @Throws(GradleException::class)
    @TaskAction
    fun prepareRelease() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version
        val versionService = versionConfig.versionService

        val version = if (versionService.localService.branchType != BranchType.tag) {
            // create tag
            val tv = versionService.preVersion.toString()

            if (!versionService.isReleaseVersionAvailable(tv)) {
                versionService.createTag(tv, null)
            }

            if (versionService.moveTo(tv, BranchType.tag) == "") {
                throw GradleException("It is not possible to move the existing working copy to version ${tv} on the SCM!")
            }
            tv
        } else {
            versionConfig.version
        }

        println("""
                    |----------------------------------------------
                    |        Project version: ${version}
                    |----------------------------------------------""".trimMargin())
    }
}