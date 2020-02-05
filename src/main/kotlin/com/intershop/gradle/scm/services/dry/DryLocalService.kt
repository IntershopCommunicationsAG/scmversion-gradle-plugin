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

import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.utils.PrefixConfig
import com.intershop.gradle.scm.utils.ScmType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class DryLocalService( projectDir: File,
                       prefixes: PrefixConfig,
                       val localService: ScmLocalService) : ScmLocalService(projectDir, prefixes) {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    /**
     * The base (stabilization) branch name of the current working copy
     */
    override val branchName: String
        get() = localService.branchName

    /**
     * This is true, if the local working copy changed.
     *
     * @property changed
     */
    override val changed: Boolean
        get() = localService.changed

    /**
     * It returns the remote url for the project, calculated
     * from the properties of the working copy (read only).
     *
     * @return remote url
     */
    override val remoteUrl: String
        get() {
            log.info("Use remote URL {}.", localService.remoteUrl)
            return localService.remoteUrl
        }

    /**
     * The revision id from the working copy (read only).
     *
     * @return revision id
     */
    override val revID: String
        get() {
            log.info("Revision ID is {}.", localService.revID)
            return localService.revID
        }
}