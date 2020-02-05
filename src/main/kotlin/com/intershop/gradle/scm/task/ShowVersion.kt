package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class ShowVersion: DefaultTask() {

    init {
        outputs.upToDateWhen { false }

        description = "Prints current project version extracted from SCM."
        group = "Release Version Plugin"
    }

    @TaskAction
    fun show() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version

        val version = versionConfig.version
        project.logger.debug("Version is {}", version)

        println("""
            |----------------------------------------------
            |        Project version: ${version}
            |----------------------------------------------""".trimMargin())
    }
}