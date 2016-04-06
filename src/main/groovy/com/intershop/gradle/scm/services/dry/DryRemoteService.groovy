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
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.gradle.scm.utils.ScmKey
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.AbstractBranchFilter

class DryRemoteService {

    /**
     * Info service based on a remote system
     */
    public final DryLocalService dryLocalService

    /**
     * Construcutor
     * @param sls  ScmLocalService contains all local information
     * @param user User credentials
     * @param key  Key credentials
     */
    DryRemoteService(ScmLocalService sls,
                     ScmUser user = null,
                     ScmKey key = null) {

        if (! sls instanceof DryLocalService) {
            throw new ScmException("Info service is not an instance of DryLocalService")
        } else {
            this.dryLocalService = (DryLocalService) sls
        }

    }

    public Map<String, BranchObject> getTagMap(AbstractBranchFilter branchFilter) {
        log.info("Not available for file system only projects!")
        return [:]
    }
}
