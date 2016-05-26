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
import com.intershop.release.version.ParserException
import com.intershop.release.version.Version
import com.intershop.release.version.VersionParser
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * <p>Gradle task 'toVersion'</p>
 * <p>It moves the working copy to a specified version. The version
 * is specified with the project property 'targetVersion'.</p>
 * <p>For a feature branch it is necessary to specify the feature key
 * with the project property 'feature'.</p>
 * <p>The task will fail if the version string can not be parsed.</p>
 */
@Slf4j
@CompileStatic
class ToVersion extends DefaultTask {

    public final static String VERSION_PROPNAME = 'targetVersion'
    public final static String FEATURE_RPOPNAME = 'feature'

    public ToVersion() {
        this.outputs.upToDateWhen { false }
    }

    @TaskAction
    void toVersion() {
        VersionExtension versionConfig = ((ScmExtension)project.extensions.getByType(ScmExtension)).version

        String targetVersion = ''
        String feature = ''

        if(project.hasProperty(VERSION_PROPNAME)) {
            targetVersion = project.property(VERSION_PROPNAME)
            log.debug('Version is {}', targetVersion)
        }
        if(project.hasProperty(FEATURE_RPOPNAME)) {
            feature = project.property(FEATURE_RPOPNAME)
            log.debug('Feature is {}', feature)
        }

        if(targetVersion) {
            Version v = null
            try {
                v = VersionParser.parseVersion(targetVersion, versionConfig.getVersionType())
            } catch(ParserException ex) {
                log.error('The version {} is not a valid version.', v.toString())
                throw new GradleException('The target version is not valid')
            }

            if(feature) {
                v = v.setBranchMetadata(feature)
            }

            log.debug('Target version is {}', v.toString())

            ScmVersionService versionService = versionConfig.getVersionService()
            try {
                String revision = versionService.moveTo(v.toString(), feature != null && feature != '')
                log.info('Working copy was switched to {} with revision id {}', v.toString(), revision)
            } catch(Exception ex) {
                log.error('It was not possible to switch the current working copy to the specifed version.', ex)
                throw new GradleException("It was not possible to switch the current working copy to the specifed version [${ex.message}].")
            }
        }
    }
}
