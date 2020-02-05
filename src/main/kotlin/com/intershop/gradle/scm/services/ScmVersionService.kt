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
import com.intershop.gradle.scm.services.git.GitLocalService
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

abstract class ScmVersionService(val versionExt: VersionExtension) {

    companion object {
        @JvmStatic
        val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        val specialBranches = listOf(BranchType.featureBranch, BranchType.bugfixBranch, BranchType.hotfixbBranch)
        val baseBranches = listOf(BranchType.trunk, BranchType.branch, BranchType.tag)
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
            var tempVersion = if(versionExt.initialVersion.isNotEmpty()) {
                getVersionFromString(versionExt.initialVersion, "intial")
            } else {
                (Version.Builder(versionExt.versionType)).build()
            }
            return tempVersion!!
        }

    /**
     * Returns an object from the SCM with additional information.
     *
     * @property versionObject version object from scm
     */
    abstract val versionObject: ScmVersionObject

    /**
     * Moves the working copy to a specified version
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the working after the move
     */
    abstract fun moveTo(version: String, type: BranchType): String

    /**
     * Creates a tag with the specified version.
     *
     * @param version
     * @return the revision id of the tag
     */
    abstract fun createTag(version: String, rev: String?): String

    /**
     * Returns a list of version and tags/branches
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
    abstract fun createBranch(version: String, featureBranch: Boolean, rev: String?): String

    /**
     * Returns true, if the specified release version is available.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    abstract fun isReleaseVersionAvailable(version: String): Boolean

    /**
     * Returns a Map with version and associated version tag object
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

    protected fun calcVersion(): String {
        with(versionExt) {
            if (!disableSCM) {
                val tempVersion: Version = preVersion

                val revIDExtension = scmRevExtension

                if (continuousRelease && !revIDExtension.isEmpty() && ! localService.changed) {
                    log.info("Version {} will be extended with revID '{}'", tempVersion, revIDExtension)
                    return tempVersion.setBuildMetadata(revIDExtension).toString()
                }

                if (localService.branchType == BranchType.detachedHead) {
                    val versionForDetachedHead = tempVersion.setBuildMetadata(revIDExtension)

                    if (continuousRelease && !localService.changed) {
                        log.info("Version {} will be extended with revID for detached head '{}'",
                                tempVersion, revIDExtension)
                        return versionForDetachedHead.toString()
                    } else {
                        log.info("Version {} will be extended with revID for detached head and SNAPSHOT '{}'",
                                tempVersion, revIDExtension)
                        return "${versionForDetachedHead}-${SNAPSHOT}"
                    }
                }

                if (!versionObject.changed) {
                    log.info("Version {} will be used without extension (No changes detected!).", tempVersion)
                    return tempVersion.toString()
                }
                if (useBuildExtension) {
                    log.info("Version {} will be extended with SNAPSHOT", tempVersion)
                    return "${tempVersion}-${SNAPSHOT}"
                } else {
                    log.info("Version {} will be extended with SNAPSHOT.", tempVersion.normalVersion)
                    return tempVersion.setBuildMetadata(SNAPSHOT.toString()).toString()
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

    protected fun calcPreVersion(): Version {
        with(versionObject) {
            log.info("Version analysis: Path: {}, version: {}, changed: {}, metadata: {}, fromBranch: {}, default: {}",
                    scmPath, version, changed, version.buildMetadata, fromBranchName, defaultVersion)

            if ((!changed && fromBranchName) || defaultVersion)  return version

            if (! specialBranches.contains(localService.branchType)) {
                if (changed) {
                    if (versionExt.increment.isEmpty() &&
                            localService.branchType != BranchType.trunk) {
                        return version.incrementVersion()
                    }
                    if (versionExt.increment.isEmpty() &&
                            localService.branchType == BranchType.trunk) {
                        return version.incrementLatest()
                    }
                    if (versionExt.increment.isNotEmpty()) {
                        val pos = DigitPos.valueOf(versionExt.increment)
                        return version.incrementVersion(pos)
                    }
                }
            } else {
                if (versionExt.majorVersionOnly) {
                    val tv = version
                    var mv = tv.normalVersion.getMajor()
                    if (versionExt.increment == "MAJOR") ++mv
                    return Version.forIntegers(mv, tv.normalVersion.versionType)
                            .setBranchMetadata(tv.getBranchMetadata().toString())
                } else if (changed) {
                    if (version.buildMetadata != MetadataVersion(null)) {
                        return version.incrementBuildMetadata()
                    } else {
                        return version.setBuildMetadata(versionExt.defaultBuildMetadata)
                    }
                }
            }
            return version
        }
    }

    val scmRevExtension: String by lazy {
        calcScmRevExtension()
    }

    protected fun calcScmRevExtension(): String {
        if(localService.branchType == BranchType.trunk ||
                versionExt.continuousReleaseBranches.contains(localService.branchName) ||
                localService.branchType == BranchType.detachedHead) {
            return if(localService is GitLocalService) {
                         "rev.id." + localService.revID.substring(0,7)
                    } else {
                        "rev.id." + localService.revID
                    }
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
            return rv
        }

    /**
     * Calculates the previous version from the scm.
     * If no release tag is available, the return value is null
     * @property previous version from the scm repository
     */
    val previousVersion: Version? by lazy {
            val tagMap : Map<Version, VersionTag> = versionTagMap
            val versions: List<Version> = if( versionExt.useBuildExtension ) {
                tagMap.keys.toList().sorted()
            } else {
                tagMap.keys.filter { it.buildMetadata.isEmpty() }.toList().sorted()
            }
            val resultList = versions.filter { it < preVersion }
            if(resultList.isEmpty()) { null } else { resultList.last() }
        }

    /**
     * Returns a previous version with the associated tag. If no tag is available, an exception will be thrown.
     * @param targetVersion
     * @return
     */
     fun getPreviousVersionTag(prevVersionStr: String): VersionTag {
        var returnValue: VersionTag? = null
        var errormessage = "The configured previous version is not available!"

        if(prevVersionStr.isNotEmpty()) {
            val previousVersionObj = getVersionFromString(prevVersionStr, "previous")
            returnValue = if (previousVersionObj != null) { versionTagMap.get(previousVersionObj) } else { null }
        } else {
            errormessage = "There is no previous version!"
            if(previousVersion != null) {
                val tagMap : Map<Version, VersionTag> = versionTagMap
                returnValue = tagMap.get(previousVersion!!)
            }
        }

        if(returnValue == null) {
            throw GradleException("$errormessage Please check your configuration.")
        }

        return returnValue
    }

    /**
     * Method for parsing version strings
     * @param versionStr
     * @param versionKind
     * @return version object
     */
    fun getVersionFromString(versionStr: String, versionKind: String): Version? {
        try {
            return Version.valueOf(versionStr)
        }catch(pe: ParserException) {
            log.warn("It was not possible to parse the {} version from configured value.", versionKind)
        }
        return null
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
        if(type == BranchType.tag &&
                ! localService.prefixes.tagPrefixSeperator.isNullOrBlank()) {
            return "${localService.prefixes.getPrefix(type)}${localService.prefixes.tagPrefixSeperator}${version}"
        }
        if((type == BranchType.branch || type == BranchType.featureBranch) &&
                ! localService.prefixes.branchPrefixSeperator.isNullOrBlank() ) {
            return "${localService.prefixes.getPrefix(type)}${localService.prefixes.branchPrefixSeperator}${version}"
        }
        return "${localService.prefixes.getPrefix(type)}${localService.prefixes.prefixSeperator}${version}"
    }
}
