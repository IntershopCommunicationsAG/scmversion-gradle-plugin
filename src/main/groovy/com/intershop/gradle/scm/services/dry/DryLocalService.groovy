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

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.services.ScmLocalService
import groovy.util.logging.Slf4j
/**
 * This is the implementation of a ScmInfoService for the dryRun mode.
 * It uses the info service, which has been selected on the basis of the working copy of the project.
 */
@Slf4j
class DryLocalService extends ScmLocalService {

    /**
     * Info service based on the file system
     */
    public final ScmLocalService localService

    /**
     * This constructs a SCM info service from the directory and the original selected info service.
     *
     * @param projectDir
     * @param infoService
     */
    DryLocalService(File projectDir, ScmExtension scmExtension, ScmLocalService localService) {
        super(projectDir, scmExtension)

        this.localService = localService
        this.branchName = localService.branchName
        this.featureBranchName = localService.featureBranchName
        this.branchType = localService.branchType
        this.changed = localService.changed
    }

    /**
     * It returns the remote url for the project, calculated
     * from the properties of the working copy (read only).
     *
     * @return remote url
     */
    @Override
    String getRemoteUrl() {
        log.info('Use remote URL {}.', localService.getRemoteUrl())
        return localService.getRemoteUrl()
    }

    /**
     * The revision id from the working copy (read only).
     *
     * @return revision id
     */
    @Override
    String getRevID() {
        log.info('Revision ID is {}.', localService.getRevID())
        return localService.getRevID()
    }
}
