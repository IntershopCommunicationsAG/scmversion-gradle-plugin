/*
 * Copyright 2020 Intershop Communications AG.
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
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.release.version.ParserException
import com.intershop.release.version.VersionParser
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * This is the implementation of Gradle
 * task to switch the branch to a special version.
 */
open class ToVersion: AbstractDryRunTask() {

    init {
        outputs.upToDateWhen { false }

        description = "Moves the existing working copy to a specified version _tag_"
    }

    private var versionProp: String = ""
    private var branchTypeProp: String = ""
    private var featureExtProp: String = ""

    /**
     * This property is used if a special version should be specified.
     *
     * @property version
     */
    @set:Option(option = "version", description = "Target version for the toVersion task.")
    @get:Optional
    @get:Input
    var version: String
        get() = versionProp
        set(value) {
            versionProp = value
        }

    /**
     * This property is used for a special branch type.
     *
     * @property branchType
     */
    @set:Option(option = "branchType", description = "Branch type for the toVersion task.")
    @get:Optional
    @get:Input
    var branchType: String
        get() = branchTypeProp
        set(value) {
            branchTypeProp = value
        }

    /**
     * This property is used for a special branch type.
     *
     * @property feature
     */
    @set:Option(option = "feature", description = "Feature extension for version tasks.")
    @get:Optional
    @get:Input
    var feature: String
        get() = featureExtProp
        set(value) {
            featureExtProp = value
        }

    private fun getProperty(propName: String): String {
        return if( project.hasProperty(propName) ) { project.property(propName).toString() } else { "" }
    }

    private fun getBranchType(branchTypeStr: String, featureBranchStr: String): BranchType {
        return when {
            branchTypeStr.isNotEmpty()      -> BranchType.valueOf(branchTypeStr)
            featureBranchStr.isNotEmpty()   -> BranchType.FEATUREBRANCH
            else -> BranchType.BRANCH
        }
    }

    /**
     * Implementation of the task action.
     */
    @Throws(GradleException::class)
    @TaskAction
    fun toVersionAction() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version

        project.logger.debug("Version is {} branch type is {}, Feature is {}",
                versionProp, branchTypeProp, featureExtProp)

        if(versionProp.isNotEmpty()) {

            try {
                val bType = getBranchType(branchTypeProp, featureExtProp)
                var v = VersionParser.parseVersion(versionProp, versionConfig.versionType)

                if (featureExtProp.isNotEmpty()) {
                    v = v.setBranchMetadata(featureExtProp)
                }

                project.logger.debug("Target version is {}", v.toString())
                project.logger.debug("Branch type is {}", bType.toString())

                val versionService = versionConfig.versionService
                if(! dryRun) {
                    val revision = versionService.moveTo(v.toString(), bType)
                    project.logger.info("Working copy was switched to {} with revision id {}", v.toString(), revision)
                } else {"""
                    |----------------------------------------------
                    |        DryRun: Working copy will be switched
                    |        to $v with for $bType
                    |----------------------------------------------""".trimMargin()
                }
            } catch(iex: IllegalArgumentException) {
                project.logger.error("The branch type {} is not a valid type.", branchTypeProp)
                throw GradleException("The branch type is not valid")
            } catch( ex: ParserException) {
                project.logger.error("The version {} is not a valid version.", versionProp)
                throw GradleException("The target version is not valid")
            } catch( ex: ScmException) {
                project.logger.error( "It was not possible to switch the current working copy to the specifed version.",
                        ex)
                throw GradleException( "It was not possible to switch the current working copy " +
                                "to the specifed version [${ex.message}].")
            }
        }
    }
}
