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
package com.intershop.gradle.scm.utils

import java.time.LocalDateTime

object ChangeLogServiceHelper {

    fun getHeader(sourceVersion: String, targetVersion: String): String {
        return """
        = Change Log for ${sourceVersion}

        This list contains changes since ${targetVersion}. +
        Created: ${LocalDateTime.now()}

        [cols="5%,5%,90%", width="95%", options="header"]
        |===
        """
    }

    val footer: String
        get() {
        return """|===
        """
        }

    fun getLineChangedFile(path: String, changeType: String): String {
        return "| | ${changeType} | ${path} \n"
    }

    fun getLineMessage(message: String, rev: String): String {
        return "3+| ${message} (${rev}) \n"
    }
}