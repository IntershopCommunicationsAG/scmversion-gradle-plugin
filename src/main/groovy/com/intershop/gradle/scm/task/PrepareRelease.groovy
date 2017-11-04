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
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
/**
 * <p>Gradle task 'release'</p>
 * <p>It creates a tag if necessary and moved the working copy to the
 * specified version. It is not possible to create branch from an existing tag!</p>
 */
@Slf4j
@CompileStatic
class PrepareRelease extends DefaultTask {

    PrepareRelease() {
        this.outputs.upToDateWhen { false }
    }

    @TaskAction
    void prepareRelease() {
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByType(ScmExtension)).version
        ScmVersionService versionService = versionConfig.getVersionService()

        if(versionService.localService.branchType != BranchType.tag) {
            // create tag
            String version = versionService.getPreVersion().toString()

            if(! versionService.isReleaseVersionAvailable(version)){
                versionService.createTag(version)
            }

            if(versionService.moveTo(version.toString(), BranchType.tag) == '') {
                throw new GradleException("It is not possible to move the existing working copy to version ${version} on the SCM!")
            }
            versionConfig.updateVersionService()
        }

        String version = versionConfig.getVersion()

        log.debug('Version is {}', version)

        String output= """
            ----------------------------------------------
                Project version: ${version}
            ----------------------------------------------""".stripIndent()

        println output
    }
}