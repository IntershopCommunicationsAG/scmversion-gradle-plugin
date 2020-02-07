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
package com.intershop.gradle.scm.services.file

import com.intershop.gradle.scm.services.ScmChangeLogService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * File change log service implementation.
 * This is without functionality.
 */
class FileChangeLogService : ScmChangeLogService {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    /**
     * Implementation for the main function
     * without functionality.
     *
     * @param changelogFile
     * @param targetVersion
     */
    override fun createLog(changelogFile: File, targetVersion: String?) {
        log.warn("This function is unsupported scm for the change log creation.")
    }
}
