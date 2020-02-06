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
package com.intershop.gradle.scm.services.dry

import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.version.AbstractBranchFilter
import com.intershop.gradle.scm.version.ScmVersionObject
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.Version
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DryVersionService(versionExt: VersionExtension,
                        private val sls: DryLocalService,
                        private val scmVersionService: ScmVersionService): ScmVersionService(versionExt)  {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    /**
     * The basic information service of this project.
     *
     * @property localService
     */
    override val localService: ScmLocalService
        get() = sls

    /**
     * Returns an object from the SCM with additional information.
     * It uses the original remote client.
     *
     * @return version object from scm
     */
    override val versionObject: ScmVersionObject
        get () {
            return scmVersionService.versionObject
        }

    /**
     * Returns a Map with version and associated version tag object
     *
     * @property versionTagMap map of version and version tag
     */
    override val versionTagMap: Map<Version, VersionTag>
        get() {
            return scmVersionService.versionTagMap
        }

    /**
     * Moves the working copy to a specified version
     * This shows only the version if available.
     *
     * @param version
     * @param type Branchtype of the target branch
     * @return the revision id of the working after the move
     */
    override fun moveTo(version: String, type: BranchType): String {
        if(scmVersionService.isReleaseVersionAvailable(version)) {
            log.info("Move working dir to {}", version)
        }
        return ""
    }

    /**
     * Creates a tag with the specified version.
     * This shows only the version for the new tag.
     *
     * @param version
     * @return the revision id of the tag
     */
    override fun createTag(version: String, rev: String?): String {
        log.info("Create tag for {}", version)
        return ""
    }

    /**
     * Returns a list of version and tags/branches
     * @param branchFilter
     * @return map
     */
    override fun getTagMap(branchFilter: AbstractBranchFilter): Map<String, BranchObject> {
        return scmVersionService.getTagMap(branchFilter)
    }

    /**
     * Creates a branch with the specified version.
     * This shows only the version for the new branch.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the branch
     */
    override fun createBranch(version: String, featureBranch: Boolean, rev: String?): String {
        log.info("Create feature branch for {}", version)
        return ""
    }

    /**
     * Returns true, if the specified release version is available.
     * This uses the original remote client.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    @Override
    override fun isReleaseVersionAvailable(version: String): Boolean {
        return scmVersionService.isReleaseVersionAvailable(version)
    }
}
