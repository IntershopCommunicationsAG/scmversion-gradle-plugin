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
package com.intershop.gradle.scm.services.file

import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.ScmVersionObject
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.Version
import groovy.util.logging.Slf4j

/**
 * This is the container for the remote access to the used SCM of a project.
 * It calculates the version and has methods to create a branch, a tag or
 * move the working copy to a special version.
 * This implementation is only connected to local file system.
 */
@Slf4j
class FileVersionService extends FileRemoteService implements ScmVersionService {

    /**
     * Constructs a valid remote client for SCM access.
     *
     * @param infoService information from the working copy
     * @param versionBranchType branch type which is used for version calculation
     * @param versionType version type - three or four version digits
     * @param patternDigits Number of digits used for the search pattern of valid branches
     * with version information.
     */
    FileVersionService(ScmLocalService sls,
                       ScmUser user = null) {
        super(sls, user)
        localService = sls
    }

    /**
     * Returns an object from the SCM with additional information.
     * For this implementation it returns always the default version.
     *
     * @return version object from scm
     */
    @Override
    ScmVersionObject getVersionObject() {
        ScmVersionObject svo = new ScmVersionObject(this.localService.projectDir.getName(), getDefaultVersion(), true)
        svo.defaultVersion = true
        return svo
    }

    public Map<Version, VersionTag> getVersionTagMap() {
        log.info("Not available for file system only projects!")
        return [:]
    }

    /**
     * Moves the working copy to a specified version.
     * This is not available for this implementation.
     *
     * @param version
     * @param type Branchtype of the target branch
     * @return the revision id of the working after the move
     */
    @Override
    String moveTo(String tagName, BranchType type) {
        log.info("Not available for file system only projects!")
        return ''
    }

    /**
     * Creates a tag with the specified version.
     * This is not available for this implementation.
     *
     * @param version
     * @return the revision id of the tag
     */
    @Override
    String createTag(String version) {
        log.info("Not available for file system only projects!")
        return ''
    }

    /**
     * Creates a branch with the specified version.
     * This is not available for this implementation.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the branch
     */
    @Override
    String createBranch(String version, boolean featureBrach) {
        log.info("Not available for file system only projects!")
        return ''
    }

    /**
     * Returns true, if the specified release version is available.
     * This returns always false for this implementation.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    @Override
    public boolean isReleaseVersionAvailable(String version) {
        log.info("Not available for file system only projects!")
        return false
    }
}
