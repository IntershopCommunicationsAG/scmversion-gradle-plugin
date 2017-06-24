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
package com.intershop.gradle.scm.services.dry

import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.version.ScmVersionObject
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.Version
import groovy.util.logging.Slf4j

/**
 * This is the container for the remote access to the used SCM of a project.
 * It calculates the version and has methods to create a branch, a tag or
 * move the working copy to a special version.
 * It uses the remote client, which has been selected on the basis
 * of the working copy of the project.
 */
@Slf4j
class DryVersionService extends DryRemoteService implements ScmVersionService {

    /*
     * Info service for this remote client
     */
    private DryLocalService dryLocalService

    /*
     * this is the original remote client based on the used SCM
     */
    private ScmVersionService remoteClient

    /**
     * Constructs a valid remote client for SCM access.
     *
     * @param infoService information from the working copy
     * @param versionBranchType branch type which is used for version calculation
     * @param versionType version type - three or four version digits
     * @param patternDigits Number of digits used for the search pattern of valid branches
     * with version information.
     */
    DryVersionService(ScmLocalService sls,
                      ScmVersionService scmRemoteService) {

        super(sls, null, null)
        localService = sls
        this.remoteClient = scmRemoteService
    }

    /**
     * Returns an object from the SCM with additional information.
     * It uses the original remote client.
     *
     * @return version object from scm
     */
    @Override
    ScmVersionObject getVersionObject() {
        return remoteClient.getVersionObject()
    }

    Map<Version, VersionTag> getVersionTagMap() {
        return remoteClient.getVersionTagMap()
    }

    /**
     * Moves the working copy to a specified version
     * This shows only the version if available.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the working after the move
     */
    @Override
    String moveTo(String version, boolean featureBrach) {
        if(remoteClient.isReleaseVersionAvailable(version)) {
            log.info("Move working dir to ${version}")
        }
        return ''
    }

    /**
     * Creates a tag with the specified version.
     * This shows only the version for the new tag.
     *
     * @param version
     * @return the revision id of the tag
     */
    @Override
    String createTag(String version) {
        log.info("Create tag for ${version}")
        return ''
    }

    /**
     * Creates a branch with the specified version.
     * This shows only the version for the new branch.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the branch
     */
    @Override
    String createBranch(String version, boolean featureBrach) {
        if(featureBrach) {
            log.info("Create feature branch for ${version}")
        } else {
            log.info("Create branch for ${version}")
        }
        return ''
    }

    /**
     * Returns true, if the specified release version is available.
     * This uses the original remote client.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    @Override
    boolean isReleaseVersionAvailable(String version) {
        return remoteClient.isReleaseVersionAvailable(version)
    }
}
