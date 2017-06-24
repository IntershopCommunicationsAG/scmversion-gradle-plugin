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
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.ScmException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
/**
 * <p>Gradle task 'tag'</p>
 * <p>It creates a tag on the SCM based on the SCM remote client. It is
 * not possible to create branch from an existing tag!</p>
 */
@Slf4j
@CompileStatic
class CreateTag extends DefaultTask {

    CreateTag() {
        this.outputs.upToDateWhen { false }
    }

    @TaskAction
    void tag() {
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByType(ScmExtension)).version
        ScmVersionService versionService = versionConfig.getVersionService()

        if(versionService.localService.branchType == BranchType.tag) {
            log.error('It is not possible to create a tag from an existing tag! Please check your working copy.')
            throw new GradleException('It is not possible to create a tag from an existing tag! Please check your working copy.')
        }

        String version = versionService.getPreVersion().toString()
        String newRev = ''
        try {
            newRev = versionService.createTag(version)
        } catch (ScmException se) {
            throw new GradleException(se.getMessage())
        }

        if (! newRev) {
            log.error('It is not possible to create a tag!')
            throw new GradleException('It is not possible to create a tag on the SCM!')
        } else {
            String output= """
                ----------------------------------------------
                    Tag created: ${version}
                ----------------------------------------------""".stripIndent()

            println output
        }
    }
}
