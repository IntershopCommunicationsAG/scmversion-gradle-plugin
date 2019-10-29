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
package com.intershop.gradle.scm.builder

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.services.ScmChangeLogService
import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.services.dry.DryLocalService
import com.intershop.gradle.scm.services.dry.DryVersionService
import com.intershop.gradle.scm.services.file.FileChangeLogService
import com.intershop.gradle.scm.services.file.FileLocalService
import com.intershop.gradle.scm.services.file.FileVersionService
import com.intershop.gradle.scm.services.git.GitChangeLogService
import com.intershop.gradle.scm.services.git.GitLocalService
import com.intershop.gradle.scm.services.git.GitVersionService
import com.intershop.gradle.scm.utils.ScmType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Creates the SCM objects from the specified project.
 */
@Slf4j
@CompileStatic
class ScmBuilder {

    // forbid instantiation
    private ScmBuilder() {}

    /**
     * Get local service
     * @param project
     * @return
     */
    static ScmLocalService getScmLocalService(Project project) {
        ScmExtension scmExt = getScmExtension(project)

        ScmLocalService service = scmExt.scmType == ScmType.git ?
                new GitLocalService(project.getRootProject().getRootDir(), scmExt) :
                new FileLocalService(project.getRootProject().getRootDir(), scmExt)

        return service
    }


    /**
     * Returns the info service from the local project.
     *
     * @param project current project
     * @return the info service of the project
     */
    static ScmVersionService getScmVersionService(Project project, VersionExtension versionExt) {

        ScmExtension scmExt = getScmExtension(project)

        ScmLocalService sls = getScmLocalService(project)

        ScmVersionService service = scmExt.scmType == ScmType.git ?
                new GitVersionService(sls, scmExt.user, scmExt.key) :
                new FileVersionService(sls, scmExt.user)

        service.versionExt = versionExt

        if(versionExt.dryRun == true) {
            sls = new DryLocalService(project.projectDir , scmExt, sls)
            return new DryVersionService(sls, service)
        }

        return service
    }

    /**
     * Returns the info change log service from the local project.
     *
     * @param project current project
     * @return the change log service of the project
     */
    static ScmChangeLogService getScmChangeLogService(Project project) {

        ScmExtension scmExt = getScmExtension(project)
        VersionExtension versionExt = getScmExtension(project).version

        ScmLocalService sls = getScmLocalService(project)

        ScmChangeLogService service = scmExt.scmType == ScmType.git ?
                new GitChangeLogService(sls, versionExt, scmExt.user, scmExt.key) :
                new FileChangeLogService(sls, versionExt, scmExt.user, scmExt.key)

        return service
    }

    private static ScmExtension getScmExtension(Project project) {
        ScmExtension scmConfig = project.extensions.getByType(ScmExtension)
        if(! scmConfig) {
            throw new GradleException("Extension 'scm' does not exists.")
        }
        return scmConfig
    }
}
