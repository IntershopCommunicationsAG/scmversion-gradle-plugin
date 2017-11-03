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
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.AbstractBranchFilter
import groovy.util.logging.Slf4j

@Slf4j
class FileRemoteService {

    private final FileLocalService localService

    FileRemoteService(ScmLocalService sls,
                      ScmUser user = null) {
        if (! sls instanceof FileLocalService) {
            throw new ScmException("Info service is not an instance of FileLocalService")
        } else {
            this.localService = (FileLocalService) sls
        }
    }

    static Map<String, BranchObject> getTagMap(AbstractBranchFilter branchFilter) {
        log.info("Not available for file system only projects!")
        return [:]
    }
}
