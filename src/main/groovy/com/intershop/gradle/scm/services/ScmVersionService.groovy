/*
 * Copyright 2015 Intershop Communications AG.
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

import static com.intershop.release.version.VersionExtension.LOCAL
import static com.intershop.release.version.VersionExtension.SNAPSHOT

import org.gradle.api.GradleException

import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.services.git.GitLocalService
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.version.ScmBranchFilter
import com.intershop.gradle.scm.version.ScmVersionObject
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.DigitPos
import com.intershop.release.version.ParserException
import com.intershop.release.version.Version

import groovy.util.logging.Slf4j

/**
 * This is the container for the remote access to the used SCM of a project.
 * It calculates the version and has methods to create a branch, a tag or
 * move the working copy to a special version.
 */
@Slf4j
trait ScmVersionService {

    //holds the calculated version object
    private ScmVersionObject versionObject

    /**
     * Version configuration
     */
    VersionExtension versionExt

    /**
     * The basic information service of this project.
     */
    ScmLocalService localService

    /**
     * Returns a default version if no other version is available.
     *
     * @return get default version
     */
    Version getDefaultVersion() {
        if(versionExt.getInitialVersion()) {
            return getVersionFromString(versionExt.getInitialVersion(), 'intial')
        }
        return (new Version.Builder(versionExt.getVersionType())).build()
    }

    /**
     * Returns an object from the SCM with additional information.
     *
     * @return version object from scm
     */
    abstract ScmVersionObject getVersionObject()

    /**
     * Moves the working copy to a specified version
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the working after the move
     */
    abstract String moveTo(String version, BranchType type)

    /**
     * Creates a tag with the specified version.
     *
     * @param version
     * @return the revision id of the tag
     */
    abstract String createTag(String version)

    /**
     * Creates a branch with the specified version.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the branch
     */
    abstract String createBranch(String version, boolean featureBranch)

    /**
     * Returns true, if the specified release version is available.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    abstract boolean isReleaseVersionAvailable(String version)

    /**
     * Returns a Map with version and associated version tag object
     *
     * @return map of version and version tag
     */
    abstract Map<Version, VersionTag> getVersionTagMap()

    /**
     * <p>Calculates the final version string with an extension if
     * necessary.</p>
     *
     * <p><b>LOCAL</b> - Working copy with changes on a working station.</p>
     * <p><b>SNAPSHOT</b> - There are changes between the current revision
     * and the latest version branch on the CI server.</p>
     * @return  version string with extension if necessary
     */
    String getVersion() {
        if(! versionExt.disableSCM) {
            // store version object
            if (!versionObject) {
                versionObject = getVersionObject()
            }

            Version version = getPreVersion()

            if(versionExt.runOnCI) {
                String revIDExtension = getSCMRevExtension()
                if (versionExt.continuousRelease && ! revIDExtension.isEmpty() && ! localService.changed) {
                    log.info('Version {} will be extended with revID "{}"', version, revIDExtension)
                    return version.setBuildMetadata(revIDExtension)
                }

                if (localService.branchType == BranchType.detachedHead) {
                    Version versionForDetachedHead = version.setBuildMetadata(revIDExtension)

                    if(versionExt.isContinuousRelease() && ! localService.changed ) {
                        log.info('Version {} will be extended with revID for detached head "{}"', version, revIDExtension)
                        return versionForDetachedHead.toString()
                    } else {
                        log.info('Version {} will be extended with revID for detached head and SNAPSHOT"{}"', version, revIDExtension)
                        return "${versionForDetachedHead}-${SNAPSHOT}"
                    }
                }

                if(!versionObject.isChanged()) {
                    log.info('Version {} will be used without extension (No changes detected!).', version)
                    return version
                }
                if(versionExt.useBuildExtension) {
                    log.info('Version {} will be extended with SNAPSHOT', version)
                    return "${version}-${SNAPSHOT.toString()}"
                } else {
                    log.info('Version {} will be extended with SNAPSHOT', version.normalVersion)
                    return version.setBuildMetadata(SNAPSHOT.toString())
                }

            } else if (!versionObject.isChanged() && versionExt.runOnCI) {
                log.info('Version {} will be used without extension (No changes detected!).', version)
                return version
            } else {
                if (versionExt.useBuildExtension) {
                    log.info('Version {} will be extended with LOCAL', version)
                    return "${version}-${LOCAL.toString()}"
                } else {
                    log.info('Version {} will be extended with LOCAL', version.normalVersion)
                    return version.setBuildMetadata(LOCAL.toString())
                }
            }
        } else {
            if(versionExt.runOnCI) {
                String baseVer = versionExt.initialVersion
                switch (versionExt.getVersionExt()) {
                    case 'SNAPSHOT':
                        return "${baseVer}-${SNAPSHOT.toString()}"
                        break
                    case 'RELEASE':
                        return baseVer
                        break
                    default:
                        Date now = new Date()
                        return "${baseVer}-${now.format('yyyyMMddHHmmss')}"
                        break
                }
            } else {
                return "${versionExt.initialVersion}-${LOCAL.toString()}"
            }
        }
    }

    String getSCMRevExtension() {
        if(localService.getBranchType() == BranchType.trunk ||
                versionExt.continuousReleaseBranches.contains(localService.getBranchName()) ||
                localService.getBranchType() == BranchType.detachedHead) {
            if(localService instanceof GitLocalService) {
                return "rev.id." + localService.getRevID().substring(0,7)
            } else {
                return "rev.id." + localService.getRevID()
            }

        }

        return ""
    }

    /**
     * <p>Calculated version from the SCM. The specified digit is
     * incremented when the code was changed after the latest
     * version branch.</p>
     *
     * @return version object
     */
    Version getPreVersion() {
        // store version object
        if(! versionObject) {
            versionObject = getVersionObject()
        }

        log.info('Version analysis: Path: {}, version: {}, changed: {}, metadata: {}, fromBranch: {}, default: {}',
                versionObject.scmPath, versionObject.version, versionObject.changed, versionObject.version.buildMetadata,
                versionObject.isFromBranchName(), versionObject.defaultVersion)

        // In case the branch or tag contains a version and nothing has changed, then we don't increase the versions.
        if (localService.withVersion && !versionObject.changed) {
            return versionObject.version
        }
        
        if((! versionObject.changed && versionObject.isFromBranchName()) || versionObject.defaultVersion) {
            return versionObject.version
        }

        if(localService.branchType != BranchType.featureBranch && localService.branchType != BranchType.bugfixBranch && localService.branchType != BranchType.hotfixbBranch) {
            if(versionObject.changed) {
                if (! versionExt.increment && localService.branchType != BranchType.trunk) {
                    return versionObject.version.incrementVersion()
                }
                if (! versionExt.increment && localService.branchType == BranchType.trunk) {
                    return versionObject.version.incrementLatest()
                }
                if(versionExt.increment) {
                    DigitPos pos = Enum.valueOf(DigitPos, versionExt.increment)
                    return versionObject.version.incrementVersion(pos)
                }
            }
        } else {
            if(versionExt.majorVersionOnly) {
                Version tv = versionObject.version
                int mv = tv.normalVersion.getMajor()

                if(versionExt.increment == 'MAJOR') {
                    ++mv
                }
                return Version.forIntegers(mv, tv.normalVersion.versionType).setBranchMetadata(tv.getBranchMetadata())
            } else if (versionObject.changed) {
                if (versionObject.version.buildMetadata) {
                    return versionObject.version.incrementBuildMetadata()
                } else {
                    return versionObject.version.setBuildMetadata(versionExt.getDefaultBuildMetadata())
                }
            }
        }
        return versionObject.version
    }

    /**
     * If it is not possible to calculate a version from the SCM,
     * this method create the default SCM version object.
     * If this is also not possible a version '99.99.0.(0)' will be created.
     *
     * @return SCM version object with the initial version
     */
    ScmVersionObject getFallbackVersion() {
        log.warn('It is not possible to identify the correct version. The default value {} will be used', defaultVersion)
        ScmVersionObject rv =  new ScmVersionObject(localService.branchName, getDefaultVersion(), true)
        rv.defaultVersion = true

        return rv
    }

    /**
     * Calculates the previous version from the scm.
     * If no release tag is available, the return value is null
     * @return previous version from the scm repository
     */
    Version getPreviousVersion() {
        Map<Version, VersionTag> tagMap = getVersionTagMap()
        Set<Version> versions = versionExt.useBuildExtension ? tagMap.keySet().sort() : tagMap.keySet().findAll {  ! it.buildMetadata }.sort()

        Version previousVersion = versions.findAll { ((Version)it) < getPreVersion() }.max()
        return previousVersion
    }

    /**
     * Returns a previous version with the associated tag. If no tag is available, an exception will be thrown.
     * @param targetVersion
     * @return
     */
    VersionTag getPreviousVersionTag(String previousVersion) {

        Version previousVersionObj = null
        VersionTag versionTag = null
        if(previousVersion) {
            previousVersionObj = getVersionFromString(previousVersion, 'previous')

            if (previousVersionObj) {
                Map<Version, VersionTag> tagMap = getVersionTagMap()
                versionTag = tagMap.get(previousVersionObj)
            }
            if(versionTag) {
                return  versionTag
            } else {
                throw new GradleException("The configured previous version is not available! Please check your configuration.")
            }
        } else {
            previousVersionObj = getPreviousVersion()

            if(previousVersionObj) {
                Map<Version, VersionTag> tagMap = getVersionTagMap()
                versionTag = tagMap.get(previousVersionObj)
                return versionTag
            }
            throw new GradleException("There is no previous version! Please check your configuration.")
        }
    }

    /**
     * Method for parsing version strings
     * @param versionStr
     * @param versionKind
     * @return version object
     */
    Version getVersionFromString(String versionStr, String versionKind) {
        try {
            if(versionStr) {
                return Version.valueOf(versionStr)
            }
        }catch(ParserException pe) {
            log.warn('It was not possible to parse the {} version from configured value.', versionKind)
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
    ScmBranchFilter getBranchFilter(BranchType type) {
        BranchType bt = type != null ? type : versionExt.getVersionBranchType()
        return new ScmBranchFilter(bt, localService.prefixes, localService.branchName, localService.branchType, localService.featureBranchName, versionExt.getPatternDigits())
    }

    /**
     * Returns the branch name calculated from the parameter.
     *
     * @param type Branch type of the working copy.
     * @param version version string
     * @return
     */
    String getBranchName(BranchType type, String version) {
        if(type == BranchType.tag && localService.prefixes.tagPrefixSeperator) {
            return "${localService.prefixes.getPrefix(type)}${localService.prefixes.tagPrefixSeperator}${version}"
        }
        if((type == BranchType.branch || type == BranchType.featureBranch) && localService.prefixes.branchPrefixSeperator ) {
            return "${localService.prefixes.getPrefix(type)}${localService.prefixes.branchPrefixSeperator}${version}"
        }
        return "${localService.prefixes.getPrefix(type)}${localService.prefixes.prefixSeperator}${version}"
    }
}
