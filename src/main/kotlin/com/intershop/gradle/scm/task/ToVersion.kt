package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.release.version.ParserException
import com.intershop.release.version.VersionParser
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class ToVersion: DefaultTask() {

    companion object {
        const val VERSION_PROPNAME = "targetVersion"
        const val BRANCHTYPE_PROPNAME = "branchType"
        const val FEATURE_RPOPNAME = "feature"
    }
    
    init {
        outputs.upToDateWhen { false }

        description = "Moves the existing working copy to a specified version _tag_"
        group = "Release Version Plugin"
    }

    @TaskAction
    fun toVersion() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version

        val targetVersion = if( project.hasProperty(VERSION_PROPNAME) ) {
                project.property(VERSION_PROPNAME).toString()
            } else { "" }

        val feature = if( project.hasProperty(FEATURE_RPOPNAME)) {
                project.property(FEATURE_RPOPNAME).toString()
            } else { "" }

        val branchType = if(project.hasProperty(BRANCHTYPE_PROPNAME)) {
                project.property(BRANCHTYPE_PROPNAME).toString()
            } else { "" }

        project.logger.debug("Version is {} branch type is {}, Feature is {}", targetVersion, branchType, feature)

        if(targetVersion.isNotEmpty()) {

            try {
                val bType: BranchType = if (branchType.isNotEmpty()) {
                    BranchType.valueOf(branchType)
                } else {
                    if (feature.isNotEmpty()) {
                        BranchType.featureBranch
                    } else {
                        BranchType.branch
                    }
                }

                var v = VersionParser.parseVersion(targetVersion, versionConfig.versionType)
                if (feature.isNotEmpty()) {
                    v = v.setBranchMetadata(feature)
                }

                project.logger.debug("Target version is {}", v.toString())
                project.logger.debug("Branch type is {}", bType.toString())

                val versionService = versionConfig.versionService
                val revision = versionService.moveTo(v.toString(), bType)

                project.logger.info("Working copy was switched to {} with revision id {}", v.toString(), revision)

            } catch(iex: IllegalArgumentException) {
                project.logger.error("The branch type {} is not a valid type.", branchType)
                throw GradleException("The branch type is not valid")
            } catch( ex: ParserException) {
                project.logger.error("The version {} is not a valid version.", targetVersion)
                throw GradleException("The target version is not valid")
            } catch( ex: ScmException) {
                project.logger.error("It was not possible to switch the current working copy to the specifed version.", ex)
                throw GradleException("It was not possible to switch the current working copy to the specifed version [${ex.message}].")
            }
        }
    }
    
}