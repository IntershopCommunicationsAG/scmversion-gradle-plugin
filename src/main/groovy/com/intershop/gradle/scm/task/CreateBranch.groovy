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
import com.intershop.release.version.Version
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
/**
 * <p>Gradle task 'branch'</p>
 * <p>It creates a branch on the SCM based on the SCM remote client. It is
 * not possible to create branch from an existing tag!</p>
 * <p>For a feature branch it is necessary to specify the project property
 * 'feature'. This should be a short key for the feature branch.</p>
 */
@Slf4j
@CompileStatic
class CreateBranch extends DefaultTask {

    public final static String PROPNAME = 'feature'

    public CreateBranch() {
        this.outputs.upToDateWhen { false }
    }

    @TaskAction
    void branch() {
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByType(ScmExtension)).version
        ScmVersionService versionService = versionConfig.getVersionService()

        if(versionService.localService.branchType == BranchType.tag) {
            throw new GradleException('It is not possible to create a branch from an tag! Please check your working copy and workflow.')
        }

        Version version = versionService.getPreVersion()
        boolean isFeatureBranch = false

        if(project.hasProperty(PROPNAME) && project.property(PROPNAME)) {
            String feature = project.property(PROPNAME)
            if(versionService.localService.branchType == BranchType.branch || versionService.localService.branchType == BranchType.featureBranch) {
                version = versionService.getPreVersion()
            }
            version = version.setBranchMetadata(feature)
            isFeatureBranch = true
        }

        String newRev = ''
        try {
            newRev = versionService.createBranch(version.toStringFor(versionConfig.patternDigits), isFeatureBranch)
        }catch (Exception ex) {
            throw new GradleException("It is not possible to create a branch on the SCM! (${ex.message})")
        }

        doLast {
            if (!newRev) {
                log.error('It is not possible to create a tag!')
                throw new GradleException('It is not possible to create a tag on the SCM!')
            } else {
                println '----------------------------------------------'
                println ''
                println "Branch created: ${version}"
                println '----------------------------------------------'
            }
        }
    }
}
