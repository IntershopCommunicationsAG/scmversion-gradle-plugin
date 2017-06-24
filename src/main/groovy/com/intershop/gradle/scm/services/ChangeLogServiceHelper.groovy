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
package com.intershop.gradle.scm.services

import com.intershop.gradle.scm.utils.ScmType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project

@CompileStatic
class ChangeLogServiceHelper {

    static String getHeader(String sourceVersion, String targetVersion) {
        return """
        = Change Log for ${sourceVersion}

        This list contains changes since version ${targetVersion}. +
        Created: ${new Date()}

        [cols="5%,5%,90%", width="95%", options="header"]
        |===
        """.stripIndent()
    }

    static String getFooter() {
        return """|===
        """.stripIndent()
    }

    static String getLineChangedFile(String path, String changeType) {
        return "| | ${changeType} | ${path} \n"
    }

    static String getLineMessage(String message, String rev) {
        return "3+| ${message} (${rev}) \n"
    }
}
