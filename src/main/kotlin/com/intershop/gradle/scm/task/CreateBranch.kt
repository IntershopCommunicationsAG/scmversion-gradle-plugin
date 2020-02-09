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
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * This is the implementation of Gradle
 * task to create the "next" branch.
 */
open class CreateBranch: AbstractDryRunTask() {

    private var featureName: String = ""

    init {
        outputs.upToDateWhen { false }

        description = "Creates an SCM branch With a specific version from the working copy"
    }

    /**
     * This will configure the feature extension of a branch.
     * It is a command line option of this task.
     *
     * @property feature
     */
    @set:Option(option = "feature", description = "Feature extension for version tasks.")
    @get:Optional
    @get:Input
    var feature: String
        get() = featureName
        set(value) {
            featureName = value
        }

    /**
     * Implementation of the task action.
     */
    @Throws(GradleException::class)
    @TaskAction
    fun branch() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version
        val versionService = versionConfig.versionService

        if(versionService.localService.branchType == BranchType.TAG) {
            throw GradleException("It is not possible to create a branch from an tag! " +
                    "Please check your working copy and workflow.")
        }

        var version = versionService.preVersion
        var isFeatureBranch = false

        if(featureName.isNotEmpty()) {
            version = version.setBranchMetadata(featureName)
            isFeatureBranch = true
        }

        if(! dryRun) {
            try {
                versionService.createBranch(version.toStringFor(versionConfig.patternDigits), isFeatureBranch)
            } catch (ex: Exception) {
                throw GradleException("It is not possible to create a branch on the SCM! (${ex.message})")
            }

            println("""
            |----------------------------------------------
            |        Branch created: $version
            |----------------------------------------------""".trimMargin())

        } else {

            println("""
            |----------------------------------------------
            |        DryRun: Branch would be created: 
            |        Branch: $version
            |----------------------------------------------""".trimMargin())
        }
    }
}
