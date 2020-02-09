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

import com.intershop.gradle.scm.extension.ChangeLogExtension
import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.task.CreateBranch
import com.intershop.gradle.scm.task.CreateChangeLog
import com.intershop.gradle.scm.task.CreateTag
import com.intershop.gradle.scm.task.PrepareRelease
import com.intershop.gradle.scm.task.ShowVersion
import com.intershop.gradle.scm.task.ToVersion
import com.intershop.gradle.scm.utils.ScmKey
import com.intershop.gradle.scm.utils.ScmUser
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

        /**
         * Task names for showVersion task.
         */
        const val  SHOW_VERSION_TASK = "showVersion"
        /**
         * Task names for toVersion task.
         */
        const val  TO_VERSION_TASK = "toVersion"
        /**
         * Task names for tag task.
         */
        const val  CREATE_TAG_TASK = "tag"
        /**
         * Task names for branch task.
         */
        const val  CREATE_BRANCH_TASK = "branch"
        /**
         * Task names for release task.
         */
        const val  RELEASE_TASK = "release"
        /**
         * Task names for changelog task.
         */
        const val  CHANGELOG_TASK = "changelog"

        /**
         * Environment variable, project property and system property for name of scm user.
         */
        val SCM_USER_NAME = mapOf("env" to "SCM_USERNAME", "prop" to "scmUserName")

        /**
         * Environment variable, project property and system property for password scm user.
         */
        val SCM_USER_PASSWORD = mapOf("env" to "SCM_PASSWORD", "prop" to "scmUserPasswd")


        /**
         * Key file for SCM access.
         */
        val SCM_KEY_FILE = mapOf("env" to "SCM_KEYFILE", "prop" to "scmKeyFile")
        /**
         * Passphrase of key file for SCM access.
         */
        val SCM_KEY_PASSPHRASE = mapOf("env" to "SCM_KEYPASSPHRASE", "prop" to "scmKeyPassphrase")

        /**
         * Environment variable to always disable the rev ID extension.
         */
        val SCM_DISABLE_REVIDEXT = mapOf("env" to "SCM_DISABLE_REVIDEXT", "prop" to "scmDisableRevIdExt")

        /**
         * Target version property name for
         * change log.
         */
        val  CHANGELOG_PREV_VERSION = mapOf("env" to "PREV_VERSION", "prop" to "prevVersion")

        /**
         * Changelog file property name for
         * change log.
         */
        val  CHANGELOG_FILE = mapOf("env" to "CHANGELOG_FILE", "prop" to "changelogFile")

        private const val ENVVAR = "env"
        private const val PROPVAR = "prop"
    }

    override fun apply(project: Project) {
        with(project) {
            logger.debug("Scm version plugin adds extension {} to {}", SCM_EXTENSION, name)
            val extension = extensions.findByType(ScmExtension::class.java) ?:
                    extensions.create(SCM_EXTENSION, ScmExtension::class.java)

            extensions.extraProperties.set("useSCMVersionConfig", true)

            configureKey(project, extension.key)
            configureUser(project, extension.user)
            extension.version.disableRevExt = getDisableRevIdExt(project)
            configureChangeLog(project, extension.changelog)

            tasks.maybeCreate(SHOW_VERSION_TASK, ShowVersion::class.java)
            tasks.maybeCreate(TO_VERSION_TASK, ToVersion::class.java)
            tasks.maybeCreate(CREATE_TAG_TASK, CreateTag::class.java)
            tasks.maybeCreate(CREATE_BRANCH_TASK, CreateBranch::class.java)
            tasks.maybeCreate(RELEASE_TASK, PrepareRelease::class.java)

            tasks.maybeCreate(CHANGELOG_TASK, CreateChangeLog::class.java).apply {
                provideChangelogFile(extension.changelog.changelogFileProvider)
                providePrevVersion(extension.changelog.previousVersionProvider)
            }
        }
    }

    private fun getDisableRevIdExt(project: Project): Boolean {
        val disable = getValueFrom(project, SCM_DISABLE_REVIDEXT, "false")
        return disable.toBoolean()
    }

    private fun configureChangeLog(project: Project, changelog: ChangeLogExtension ) {
        val preVersion = getValueFrom(project, CHANGELOG_PREV_VERSION, "")
        if(preVersion.isNotEmpty()) {
            changelog.previousVersion = preVersion
        }

        val changeLogFile = getValueFrom(project, CHANGELOG_FILE, "")
        if(changeLogFile.isNotEmpty()) {
            changelog.changelogFile = project.file(changeLogFile)
        }
    }

    private fun configureUser(project: Project, userconf: ScmUser) {
        val username = getValueFrom(project, SCM_USER_NAME, "")
        if(username.isNotEmpty()) {
            userconf.name = username
        }
        val password = getValueFrom(project, SCM_USER_PASSWORD, "")
        if(password.isNotEmpty()) {
            userconf.password = password
        }
    }

    private fun configureKey(project: Project, keyconf: ScmKey) {
        val keyFile = getValueFrom(project, SCM_KEY_FILE, "")
        if(keyFile.isNotEmpty() && File(keyFile).exists()) {
            keyconf.file = File(keyFile)
        }
        val keyPhrase = getValueFrom(project, SCM_KEY_PASSPHRASE, "")
        if(keyPhrase.isNotEmpty()) {
            keyconf.passphrase = keyPhrase
        }
    }

    private fun getValueFrom(project: Project, vars: Map<String,String>, defaultValue: String): String {
        return when {
            ! System.getenv(vars[ENVVAR]).isNullOrEmpty() -> System.getenv(vars[ENVVAR]).trim()
            ! System.getProperty(vars[PROPVAR]!!).isNullOrEmpty() -> System.getProperty(vars[PROPVAR]!!).trim()
            project.hasProperty(vars[PROPVAR]!!) &&  project.property(vars[PROPVAR]!!).toString().isNotEmpty() ->
                                                                project.property(vars[PROPVAR]!!).toString().trim()
            else -> defaultValue
        }
    }

}
