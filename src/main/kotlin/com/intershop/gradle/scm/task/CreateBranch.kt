package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.utils.BranchType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class CreateBranch: DefaultTask() {

    companion object {
        const val PROPNAME = "feature"
    }

    init {
        outputs.upToDateWhen { false }

        description = "Creates an SCM branch With a specific version from the working copy"
        group = "Release Version Plugin"
    }

    @Throws(GradleException::class)
    @TaskAction
    fun branch() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version
        val versionService = versionConfig.versionService

        if(versionService.localService.branchType == BranchType.TAG) {
            throw GradleException("It is not possible to create a branch from an tag! Please check your working copy and workflow.")
        }

        var version = versionService.preVersion
        var isFeatureBranch = false

        if(project.hasProperty(PROPNAME) && project.property(PROPNAME).toString().isNotEmpty()) {
            val feature = project.property(PROPNAME).toString()
            version = version.setBranchMetadata(feature)
            isFeatureBranch = true
        }

        try {
             versionService.createBranch(version.toStringFor(versionConfig.patternDigits), isFeatureBranch, null)
        }catch ( ex: Exception ) {
            throw GradleException("It is not possible to create a branch on the SCM! (${ex.message})")
        }

        println("""
            |----------------------------------------------
            |        Branch created: ${version}
            |----------------------------------------------""".trimMargin())
    }
}