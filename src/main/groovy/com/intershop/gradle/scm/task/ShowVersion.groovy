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
package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.extension.VersionExtension
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.StyledTextOutput
import org.gradle.logging.StyledTextOutputFactory

import static org.gradle.logging.StyledTextOutput.Style.Header
/**
 * <p>Gradle task 'showVersion'</p>
 * <p>It shows the calculated version from the SCM.</p>
 */
@Slf4j
@CompileStatic
class ShowVersion extends DefaultTask {

    public ShowVersion() {
        this.outputs.upToDateWhen { false }
    }

    @TaskAction
    void show() {
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByType(ScmExtension)).version

        String version = versionConfig.getVersion()
        log.debug('Version is {}', version)

        StyledTextOutput output = services.get(StyledTextOutputFactory).create(ShowVersion)
        output.withStyle(Header).println('')
        output.withStyle(Header).println("Test Project version: ${version}")
    }
}
