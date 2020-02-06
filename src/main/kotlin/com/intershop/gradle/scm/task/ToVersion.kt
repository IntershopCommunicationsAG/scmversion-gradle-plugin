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
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.release.version.ParserException
import com.intershop.release.version.VersionParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * This is the implementation of Gradle
 * task to switch the branch to a special version.
 */
open class ToVersion: DefaultTask() {

    companion object {
        /**
         * This is the project property name
         * of the target version.
         */
        const val VERSION_PROPNAME = "targetVersion"
        /**
         * This is the project property name
         * of the branch type.
         */
        const val BRANCHTYPE_PROPNAME = "branchType"
        /**
         * This is the project property name
         * of the feature property.
         */
        const val FEATURE_RPOPNAME = "feature"
    }
    
    init {
        outputs.upToDateWhen { false }

        description = "Moves the existing working copy to a specified version _tag_"
        group = "Release Version Plugin"
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

        val targetVersionProp = getProperty(VERSION_PROPNAME)
        val featureProp = getProperty(FEATURE_RPOPNAME)
        val branchTypeProp = getProperty(BRANCHTYPE_PROPNAME)

        project.logger.debug("Version is {} branch type is {}, Feature is {}",
                targetVersionProp, branchTypeProp, featureProp)

        if(targetVersionProp.isNotEmpty()) {

            try {
                val bType = getBranchType(branchTypeProp, featureProp)
                var v = VersionParser.parseVersion(targetVersionProp, versionConfig.versionType)

                if (featureProp.isNotEmpty()) {
                    v = v.setBranchMetadata(featureProp)
                }

                project.logger.debug("Target version is {}", v.toString())
                project.logger.debug("Branch type is {}", bType.toString())

                val versionService = versionConfig.versionService
                val revision = versionService.moveTo(v.toString(), bType)

                project.logger.info("Working copy was switched to {} with revision id {}", v.toString(), revision)

            } catch(iex: IllegalArgumentException) {
                project.logger.error("The branch type {} is not a valid type.", branchTypeProp)
                throw GradleException("The branch type is not valid")
            } catch( ex: ParserException) {
                project.logger.error("The version {} is not a valid version.", targetVersionProp)
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
