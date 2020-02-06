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
 * task to prepare a release from the
 * current SCM repository. It creates a
 * task switch the repo to this tag.
 */
open class PrepareRelease: DefaultTask() {

    init {
        outputs.upToDateWhen { false }

        description = "Prepare a release process, create branch and/or tag, moves the working copy to the tag"
        group = "Release Version Plugin"
    }

    /**
     * Implementation of the task action.
     */
    @Throws(GradleException::class)
    @TaskAction
    fun prepareReleaseAction() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version
        val versionService = versionConfig.versionService

        val version = if (versionService.localService.branchType != BranchType.TAG) {
            // create tag
            val tv = versionService.preVersion.toString()

            if (!versionService.isReleaseVersionAvailable(tv)) {
                versionService.createTag(tv, null)
            }

            if (versionService.moveTo(tv, BranchType.TAG) == "") {
                throw GradleException("It is not possible to move the existing working copy to version $tv on the SCM!")
            }
            tv
        } else {
            versionConfig.version
        }

        println("""
                    |----------------------------------------------
                    |        Project version: $version
                    |----------------------------------------------""".trimMargin())
    }
}
