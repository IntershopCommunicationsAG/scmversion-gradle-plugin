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

package com.intershop.gradle.scm.extension

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project

/**
 * <p>This is the extension object for the Intershop version plugin.</p>
 *
 * <pre>
 * {@code
 * changeLog {
 *    defaultTargetVersion = '1.0.0'
 * }
 * </pre>
 */

@CompileStatic
@Slf4j
class ChangeLogExtension extends AbstractExtension{

    // scm user name / token (git,svn)
    public final static String TARGETVERSION_ENV = 'TARGET_VERSION'
    public final static String TARGETVERSION_PRJ = 'targetVersion'

    // scm user name / token (git,svn)
    public final static String CHANGELOG_ENV = 'CHANGELOG_FILE'
    public final static String CHANGELOG_PRJ = 'changelogFile'

    String targetVersion

    File changelogFile

    boolean filterProject = false

    /**
     * Initialize this extension and set default values
     * If environment values are set for some keys this
     * values will be used.
     *
     * @param project
     */
    ChangeLogExtension(Project project) {
        super(project)

        if(! targetVersion) {
            targetVersion = getVariable(TARGETVERSION_ENV, TARGETVERSION_PRJ, '')
        }

        if(! changelogFile) {
            String file = getVariable(CHANGELOG_ENV, CHANGELOG_PRJ, '')
            if(file) {
                changelogFile = project.file(file)
            } else {
                changelogFile = new File(project.getBuildDir(), 'changelog/changelog.asciidoc')
            }
        }
    }



}
