/*
 * Copyright 2020 Intershop Communications AG.
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
package com.intershop.gradle.scm.utils

import java.time.LocalDateTime

/**
 * This class provides static methods
 * for change log file content.
 */
object ChangeLogServiceHelper {

    /**
     * Returns a header string for the change log file.
     */
    fun getHeader(sourceVersion: String, targetVersion: String): String {
        return """
        = Change Log for $targetVersion

        This list contains changes since ${sourceVersion}. +
        Created: ${LocalDateTime.now()}

        [cols="5%,5%,90%", width="95%", options="header"]
        |===
        """
    }

    /**
     * Returns a footer string for the change log file.
     */
    val footer: String
        get() {
        return """|===
        """
        }

    /**
     * Returns a line string of a changed file of the change log file.
     */
    fun getFileLine(path: String, changeType: String): String {
        return "| | $changeType | $path \n"
    }

    /**
     * Returns a message string of a commit of the change log file.
     */
    fun getMessageLine(message: String, rev: String): String {
        return "3+| $message (${rev}) \n"
    }
}
