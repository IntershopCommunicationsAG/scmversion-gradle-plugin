package com.intershop.gradle.scm.services.file

import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.version.AbstractBranchFilter
import com.intershop.gradle.scm.version.ScmVersionObject
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.Version

open class FileVersionService(versionExt: VersionExtension,
                              override val localService: FileLocalService): ScmVersionService(versionExt) {

    /**
     * Returns an object from the SCM with additional information.
     *
     * @property versionObject version object from scm
     */
    override val versionObject: ScmVersionObject by lazy {
            val svo = ScmVersionObject(this.localService.projectDir.getName(), defaultVersion, true)
            svo.defaultVersion = true
            svo
        }

    /**
     * Moves the working copy to a specified version
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the working after the move
     */
    override fun moveTo(version: String, type: BranchType): String {
        log.info("Not available for file system only projects!")
        return ""
    }

    /**
     * Creates a tag with the specified version.
     *
     * @param version
     * @return the revision id of the tag
     */
    override fun createTag(version: String, rev: String?): String {
        log.info("Not available for file system only projects!")
        return ""
    }

    /**
     * Returns a list of version and tags/branches
     * @param branchFilter
     * @return map
     */
    override fun getTagMap(branchFilter: AbstractBranchFilter): Map<String, BranchObject> {
        log.info("Not available for file system only projects!")
        return mapOf()
    }

    /**
     * Creates a branch with the specified version.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the branch
     */
    override fun createBranch(version: String, featureBranch: Boolean, rev: String?): String {
        log.info("Not available for file system only projects!")
        return ""
    }

    /**
     * Returns true, if the specified release version is available.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    override fun isReleaseVersionAvailable(version: String): Boolean {
        log.info("Not available for file system only projects!")
        return false
    }

    /**
     * Returns a Map with version and associated version tag object
     *
     * @property versionTagMap map of version and version tag
     */
    override val versionTagMap: Map<Version, VersionTag> by lazy {
            log.info("Not available for file system only projects!")
            mapOf<Version, VersionTag>()
        }


}