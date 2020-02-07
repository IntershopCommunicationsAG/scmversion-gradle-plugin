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
package com.intershop.gradle.scm.services

import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.services.git.GitLocalService.Companion.HASHLENGTH
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.version.AbstractBranchFilter
import com.intershop.gradle.scm.version.ScmBranchFilter
import com.intershop.gradle.scm.version.ScmVersionObject
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.DigitPos
import com.intershop.release.version.MetadataVersion
import com.intershop.release.version.ParserException
import com.intershop.release.version.Version
import com.intershop.release.version.VersionExtension.SNAPSHOT
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * This is the abstract class of a version service.
 *
 * @constructor creates a service
 * @param versionExt main extension of this plugin
 */
abstract class ScmVersionService(val versionExt: VersionExtension) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        /**
         * List of special branch types.
         */
        val specialBranches = listOf(BranchType.FEATUREBRANCH, BranchType.BUGFIXBRANCH, BranchType.HOTFIXBBRANCH)

        /**
         * List of base branch types.
         */
        val baseBranches = listOf(BranchType.MASTER, BranchType.BRANCH, BranchType.TAG)

        /**
         * Branches with rev id extension
         */
        val extBranches = listOf(BranchType.MASTER, BranchType.DETACHEDHEAD)
    }

    /**
     * The basic information service of this project.
     *
     * @property localService
     */
    abstract val localService: ScmLocalService

    /**
     * Returns a default version if no other version is available.
     *
     * @property defaultVersion get default version
     */
     val defaultVersion : Version
        get() {
            return if(versionExt.initialVersion.isNotEmpty()) {
                try {
                    Version.valueOf(versionExt.initialVersion)
                } catch (pex: ParserException) {
                    (Version.Builder(versionExt.versionType)).build()
                }
            } else {
                (Version.Builder(versionExt.versionType)).build()
            }
        }

    /**
     * Returns an object from the SCM with additional information.
     *
     * @property versionObject version object from scm
     */
    abstract val versionObject: ScmVersionObject

    /**
     * Moves the working copy to a specified version.
     *
     * @param version
     * @param type branch type
     * @return the revision id of the working after the move
     */
    abstract fun moveTo(version: String, type: BranchType): String

    /**
     * Creates a tag with the specified version.
     *
     * @param version
     * @return the revision id of the tag
     */
    abstract fun createTag(version: String): String

    /**
     * Returns a list of version and tags/branches.
     *
     * @param branchFilter
     * @return map
     */
    abstract fun getTagMap(branchFilter: AbstractBranchFilter): Map<String, BranchObject>

    /**
     * Creates a branch with the specified version.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the branch
     */
    abstract fun createBranch(version: String, featureBranch: Boolean): String

    /**
     * Returns true, if the specified release version is available.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    abstract fun isReleaseVersionAvailable(version: String): Boolean

    /**
     * Returns a Map with version and associated version tag object.
     *
     * @property versionTagMap map of version and version tag
     */
    abstract val versionTagMap: Map<Version, VersionTag>

    /**
     * <p>Calculates the final version string with an extension if
     * necessary.</p>
     *
     * <p><b>LOCAL</b> - Working copy with changes on a working station.</p>
     * <p><b>SNAPSHOT</b> - There are changes between the current revision
     * and the latest version branch on the CI server.</p>
     * @return  version string with extension if necessary
     */
    val version: String by lazy {
        calcVersion()
    }

    private fun calcVersion(): String {
        with(versionExt) {
            if (!disableSCM) {
                val tempVersion: Version = preVersion
                val revIDExtension = scmRevExtension

                if (continuousRelease && revIDExtension.isNotEmpty() && ! localService.changed) {
                    log.info("Version {} will be extended with revID '{}'", tempVersion, revIDExtension)
                    return tempVersion.setBuildMetadata(revIDExtension).toString()
                }

                if (localService.branchType == BranchType.DETACHEDHEAD) {
                    val versionForDetachedHead = tempVersion.setBuildMetadata(revIDExtension)

                    return if (continuousRelease && !localService.changed) {
                        log.info("Version {} will be extended with revID for detached head '{}'",
                                tempVersion, revIDExtension)
                        versionForDetachedHead.toString()
                    } else {
                        log.info("Version {} will be extended with revID for detached head and SNAPSHOT '{}'",
                                tempVersion, revIDExtension)
                        "${versionForDetachedHead}-${SNAPSHOT}"
                    }
                }

                if (!versionObject.changed) {
                    log.info("Version {} will be used without extension (No changes detected!).", tempVersion)
                    return tempVersion.toString()
                }
                return if (useBuildExtension) {
                    log.info("Version {} will be extended with SNAPSHOT", tempVersion)
                    "${tempVersion}-${SNAPSHOT}"
                } else {
                    log.info("Version {} will be extended with SNAPSHOT.", tempVersion.normalVersion)
                    tempVersion.setBuildMetadata(SNAPSHOT.toString()).toString()
                }
            } else {
                val timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                return when (versionExt) {
                    "SNAPSHOT" -> "${initialVersion}-${SNAPSHOT}"
                    "RELEASE" -> initialVersion
                    else -> "${initialVersion}-${timestamp}"
                }
            }
        }
    }

    /**
     * <p>Calculated version from the SCM. The specified digit is
     * incremented when the code was changed after the latest
     * version branch.</p>
     *
     * @return version object
     */
    val preVersion: Version by lazy {
        calcPreVersion()
    }

    private fun calcPreVersion(): Version {
        with(versionObject) {
            log.info("Version analysis: Path: {}, version: {}, changed: {}, metadata: {}, fromBranch: {}, default: {}",
                    scmPath, version, changed, version.buildMetadata, fromBranchName, defaultVersion)

            if ((!changed && fromBranchName) || defaultVersion)  return version

            if (! specialBranches.contains(localService.branchType)) {
                if (changed) return getChangedVersion()
            } else {
                if (versionExt.majorVersionOnly) {
                    var mv = version.normalVersion.major
                    if (versionExt.increment == "MAJOR") ++mv

                    return Version.forIntegers(mv, version.normalVersion.versionType)
                            .setBranchMetadata(version.branchMetadata.toString())
                } else if (changed) {
                    return if (version.buildMetadata != MetadataVersion(null)) {
                                version.incrementBuildMetadata()
                            } else {
                                version.setBuildMetadata(versionExt.defaultBuildMetadata)
                            }
                }
            }
            return version
        }
    }

    private fun getChangedVersion(): Version {
        with(versionObject) {
            return if (versionExt.increment.isEmpty() && localService.branchType != BranchType.MASTER) {
                        version.incrementVersion()
                    } else if (versionExt.increment.isEmpty() && localService.branchType == BranchType.MASTER) {
                        version.incrementLatest()
                    } else {
                        version.incrementVersion(DigitPos.valueOf(versionExt.increment))
                    }
        }
    }

    /**
     * Revison extension for the version.
     *
     * @property scmRevExtension
     */
    val scmRevExtension: String by lazy {
        calcScmRevExtension()
    }

    private fun calcScmRevExtension(): String {
        if(versionExt.continuousReleaseBranches.contains(localService.branchName)
                || extBranches.contains(localService.branchType)
                && ! versionExt.disableRevExt) {
            return "rev.id." + localService.revID.substring(0, HASHLENGTH)
        }

        return ""
    }

    /**
     * If it is not possible to calculate a version from the SCM,
     * this method create the default SCM version object.
     * If this is also not possible a version '99.99.0.(0)' will be created.
     *
     * @return SCM version object with the initial version
     */
    val fallbackVersion: ScmVersionObject
        get() {
            log.warn("It is not possible to identify the correct version. The default value {} will be used",
                    defaultVersion)
            val rv = ScmVersionObject(localService.branchName, defaultVersion, true)
            rv.defaultVersion = true

            if(! baseBranches.contains(localService.branchType)) {
                rv.updateVersion(rv.version.setBranchMetadata(localService.featureBranchName))
            }

            return rv
        }

    /**
     * Calculates the previous version from the scm.
     * If no release tag is available, the return value is null
     * @property previousVersion version from the scm repository
     */
    val previousVersion: Version? by lazy {
        val tagMap : Map<Version, VersionTag> = versionTagMap
        val versions: List<Version> = if( versionExt.useBuildExtension ) {
            tagMap.keys.toList().sorted() } else {
            tagMap.keys.filter { it.buildMetadata.isEmpty }.toList().sorted() }
        val resultList = versions.filter { it < preVersion }
        if(resultList.isEmpty()) { null } else { resultList.last() }
    }

    /**
     * Returns a previous version with the associated tag. If no tag is available, an exception will be thrown.
     * @param prevVersionStr
     * @return
     */
    fun getPreviousVersionTag(prevVersionStr: String): VersionTag {
        var returnValue: VersionTag? = null
        var errormessage = "The configured previous version is not available!"

        if(prevVersionStr.isNotEmpty()) {
            try {
                returnValue = versionTagMap[Version.valueOf(prevVersionStr)]
            } catch (ex: ParserException) {
                log.debug("Error during calculaten of previous version {}. {}", prevVersionStr, ex.cause)
            }
        } else {
            errormessage = "There is no previous version!"
            if(previousVersion != null) {
                val tagMap : Map<Version, VersionTag> = versionTagMap
                returnValue = tagMap[previousVersion!!]
            }
        }

        if(returnValue == null) {
            throw GradleException("$errormessage Please check your configuration.")
        }

        return returnValue
    }

    /**
     * Creates a branch filter of a special version branch type. It uses also
     * information from the info service client.
     *
     * @param type Version branch type, default is the value of the initialization method
     * @return Branch filter for the specified type.
     */
     fun getBranchFilter(type: BranchType): ScmBranchFilter {
        return ScmBranchFilter(localService.prefixes, type, localService.branchName,
                localService.branchType, localService.featureBranchName, versionExt.patternDigits)
    }

    /**
     * Returns the branch name calculated from the parameter.
     *
     * @param type Branch type of the working copy.
     * @param version version string
     * @return
     */
    fun getBranchName(type: BranchType, version: String): String {
        if(type == BranchType.TAG &&
                ! localService.prefixes.tagPrefixSeperator.isNullOrBlank()) {
            return "${localService.prefixes.getPrefix(type)}${localService.prefixes.tagPrefixSeperator}${version}"
        }
        if((type == BranchType.BRANCH || type == BranchType.FEATUREBRANCH) &&
                ! localService.prefixes.branchPrefixSeperator.isNullOrBlank() ) {
            return "${localService.prefixes.getPrefix(type)}${localService.prefixes.branchPrefixSeperator}${version}"
        }
        return "${localService.prefixes.getPrefix(type)}${localService.prefixes.prefixSeperator}${version}"
    }
}
