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

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.services.ScmLocalService
import groovy.util.logging.Slf4j
/**
 * This is the implementation of a ScmInfoService of a local file system.
 * It is used for unknown SCMs or for new projects.
 */
@Slf4j
class FileLocalService extends ScmLocalService {

    /**
     * This constructs a SCM info service. It contains all necessary information from
     * the working copy without remote access to the SCM.
     *
     * @param projectDir
     * @param prefixes
     * @param initialVersion
     */
    FileLocalService(File projectDir, ScmExtension scmExtension) {
        super(projectDir, scmExtension)
        log.warn("This project is not included in a SCM!")
        this.branchName = 'trunk'
        this.featureBranchName = ''
        this.branchType = 'trunk'
        this.changed = true
    }

    /**
     * It returns the remote url, calculated from the properties of the working copy (read only).
     * For this provider it is only the file path.
     *
     * @return remote url
     */
    @Override
    String getRemoteUrl() {
        return projectDir.toURI().toURL().toString()
    }

    /**
     * The revision id from the working copy (read only).
     * For this provider it is always 'unknown'.
     *
     * @return revision id
     */
    @Override
    String getRevID() {
        return 'unknown'
    }
}
