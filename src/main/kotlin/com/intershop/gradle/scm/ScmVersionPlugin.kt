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
package com.intershop.gradle.scm

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.task.CreateBranch
import com.intershop.gradle.scm.task.CreateChangeLog
import com.intershop.gradle.scm.task.CreateTag
import com.intershop.gradle.scm.task.PrepareRelease
import com.intershop.gradle.scm.task.ShowVersion
import com.intershop.gradle.scm.task.ToVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * <p>This plugin will apply the scmPlugin.</p>
 *
 * <p>It will calculate the version from the used
 * SCM. It support Git or Subversion.</p>
 * <p>It adds furthermore some tasks for the release and
 * SCM management of this project based on the version:
 *
 * <p><b>tag</b> - creates a tag on the SCM</p>
 * <p><b>branch</b> - creates a (stabilization or feature) branch on the SCM</p>
 * <p><b>toVersion</b> - move the working copy to the specified version (tag or branch)</p>
 * <p><b>release</b> - create a version tag on the SCM and move the working copy to this version</p>
 * <p><b>showVersion</b> - shows the calculated version</p>
 */
class ScmVersionPlugin  : Plugin<Project> {

    companion object {
        /**
         * Extension name of this plugin.
         */
        const val SCM_EXTENSION = "scm"

        // target version property name
        const val  TARGETVERSION = "TARGET_VERSION"

        // change log file
        const val  CHANGELOG = "CHANGELOG_FILE"

        /**
         * Task namen
         */
        const val  SHOW_VERSION_TASK = "showVersion"
        const val  TO_VERSION_TASK = "toVersion"
        const val  CREATE_TAG_TASK = "tag"
        const val  CREATE_BRANCH_TASK = "branch"
        const val  RELEASE_TASK = "release"
        const val  CHANGELOG_TASK = "changelog"
    }

    override fun apply(project: Project) {
        with(project) {
            logger.debug("Scm version plugin adds extension {} to {}", SCM_EXTENSION, name)
            val extension = extensions.findByType(ScmExtension::class.java) ?:
                    extensions.create(SCM_EXTENSION, ScmExtension::class.java)

            extensions.extraProperties.set("useSCMVersionConfig", true)

            extension.changelog.targetVersion = (System.getProperty(TARGETVERSION) ?: System.getenv(TARGETVERSION) ?: "").toString().trim()

            if(System.getProperty(CHANGELOG) != null) {
                extension.changelog.changelogFile = File(System.getProperty(CHANGELOG))
            }
            if(System.getenv(CHANGELOG) != null) {
                extension.changelog.changelogFile = File(System.getenv(CHANGELOG))
            }

            tasks.maybeCreate(SHOW_VERSION_TASK, ShowVersion::class.java)
            tasks.maybeCreate(TO_VERSION_TASK, ToVersion::class.java)
            tasks.maybeCreate(CREATE_TAG_TASK, CreateTag::class.java)
            tasks.maybeCreate(CREATE_BRANCH_TASK, CreateBranch::class.java)
            tasks.maybeCreate(RELEASE_TASK, PrepareRelease::class.java)

            tasks.maybeCreate(CHANGELOG_TASK, CreateChangeLog::class.java).apply {
                provideChangelogFile(extension.changelog.changelogFileProvider)
                provideTargetVersion(extension.changelog.targetVersioneProvider)
            }
        }
    }
}