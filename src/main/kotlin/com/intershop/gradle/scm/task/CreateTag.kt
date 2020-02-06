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
import com.intershop.gradle.scm.utils.BranchType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * This is the implementation of Gradle
 * task to create the "next" tag.
 */
open class CreateTag: DefaultTask() {

    init {
        outputs.upToDateWhen { false }

        description = "Creates an SCM tag With a specific version from the working copy"
        group = "Release Version Plugin"
    }

    /**
     * Implementation of the task action.
     */
    @Throws(GradleException::class)
    @TaskAction
    fun tag() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version
        val versionService = versionConfig.versionService

        if(versionService.localService.branchType == BranchType.TAG) {
            val txt = "It is not possible to create a tag from an existing tag! Please check your working copy."
            project.logger.error(txt)
            throw GradleException(txt)
        }

        // create tag
        val version = versionService.preVersion

        try {
            versionService.createTag(version.toString(), null)
        } catch ( ex: Exception ) {
            throw GradleException("It is not possible to create a tag on the SCM! (${ex.message})")
        }

        println("""
            |----------------------------------------------
            |        Tag created: $version
            |----------------------------------------------""".trimMargin())
    }
}
