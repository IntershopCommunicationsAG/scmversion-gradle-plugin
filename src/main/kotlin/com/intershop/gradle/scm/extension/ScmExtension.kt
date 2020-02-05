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
package com.intershop.gradle.scm.extension

import com.intershop.gradle.scm.services.ScmChangeLogService
import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.services.file.FileChangeLogService
import com.intershop.gradle.scm.services.file.FileLocalService
import com.intershop.gradle.scm.services.file.FileVersionService
import com.intershop.gradle.scm.services.git.GitChangeLogService
import com.intershop.gradle.scm.services.git.GitLocalService
import com.intershop.gradle.scm.services.git.GitRemoteService
import com.intershop.gradle.scm.services.git.GitVersionService
import com.intershop.gradle.scm.utils.PrefixConfig
import com.intershop.gradle.scm.utils.ScmKey
import com.intershop.gradle.scm.utils.ScmType
import com.intershop.gradle.scm.utils.ScmUser
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

/**
 * Extension for all SCM based plugins.
 */
abstract class ScmExtension {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    /**
     * Inject service of ProjectLayout (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val projectLayout: ProjectLayout

    /**
     * <p>SCM user</p>
     *
     * <p>Can be configured/overwritten with environment variables SCM_USERNAME,
     * SCM_PASSWORD; java environment SCM_USERNAME, SCM_PASSWORD or project
     * variables scmUserName, scmUserPasswd</p>
     *
     * <pre>
     * {@code
     *      user {
     *          name = ''
     *          password = ''
     *      }
     * }
     * </pre>
     */
    val user: ScmUser = objectFactory.newInstance(ScmUser::class.java)

    /**
     * <p>SCM key</p>
     *
     * <p>Can be configured/overwritten with environment variables SCM_KEYFILE,
     * SCM_KEYPASSPHRASE; java environment SCM_KEYFILE, SCM_KEYPASSPHRASE or project
     * variables scmKeyFile, scmKeyPassphrase</p>
     * <pre>
     * {@code
     *      key {
     *          file (File)
     *          passphrase = ''
     *      }
     * }
     * </pre>
     */
    val key: ScmKey = objectFactory.newInstance(ScmKey::class.java)

    /**
     * prefixes for branches and tags
     * <pre>
     * {@code
     *      prefixes {
     *          stabilizationPrefix = 'SB'
     *          featurePrefix = 'FB'
     *          tagPrefix = 'RELEASE'
     *
     *          prefixSeperator = '_'
     *      }
     * }
     * </pre>
     */
    val prefixes: PrefixConfig = objectFactory.newInstance(PrefixConfig::class.java)

    /**
     * Getter for scmType
     * @property scmType the scmType object for the detected SCM
     */
    val scmType: ScmType
        get() {
            val gitDir = projectLayout.projectDirectory.file(".git").asFile
            val isGit = gitDir.exists() && gitDir.isDirectory()
            return when(isGit) {
                true -> ScmType.git
                false -> ScmType.file
            }
        }

    val localService: ScmLocalService by lazy {
        if (scmType == ScmType.git) {
            GitLocalService(projectLayout.projectDirectory.asFile, prefixes)
        } else {
            FileLocalService(projectLayout.projectDirectory.asFile, prefixes)
        }
    }

    /**
     * This extension contains settings for version calculation and
     * read properties for the current version and previous version.
     */
    val version: VersionExtension = objectFactory.newInstance(VersionExtension::class.java, this)

    /**
     * This extension contains settings for change log configuration.
     */
    val changelog: ChangeLogExtension = objectFactory.newInstance(ChangeLogExtension::class.java, this)

    /**
     * SCM user for authentication.
     *
     * @param closure scm user (see ScmUser)
     */
    fun user(closure: Closure<ScmUser>) {
        ConfigureUtil.configure(closure, user)
    }

    /**
     * SCM user for authentication for Java/Kotlin
     *
     * @param action scm user (see ScmUser)
     */
    fun user(action: Action<in ScmUser>) {
        action.execute(user)
    }

    /**
     * SCM key configuration for authentication.
     *
     * @param closure scm user (see ScmKey)
     */
    fun key(closure: Closure<ScmKey>) {
        ConfigureUtil.configure(closure, key)
    }

    /**
     * SCM key configuration for authentication for Java/Kotlin.
     *
     * @param action scm user (see ScmKey)
     */
    fun key(action: Action<in ScmKey>) {
        action.execute(key)
    }

    /**
     * Prefix configuration for branches and tags
     *
     * @param closure prefix configuration (see PrefixConfig)
     */
    fun prefixes(closure: Closure<PrefixConfig>) {
        ConfigureUtil.configure(closure, prefixes)
    }

    /**
     * Prefix configuration for branches and tags for Java/Kotlin
     *
     * @param action prefix configuration (see PrefixConfig)
     */
    fun prefixes(action: Action<in PrefixConfig>) {
        action.execute(prefixes)
    }

    /**
     * Version extension with configuration
     *
     * @param closure version extension (VersionExtension)
     */
    fun version(closure: Closure<VersionExtension>) {
        ConfigureUtil.configure(closure, version)
    }

    /**
     * Version extension with configuration for Java/Kotlin
     *
     * @param action version extension (VersionExtension)
     */
    fun version(action: Action<in VersionExtension>) {
        action.execute(version)
    }

    /**
     * Changelog extenions with configuration
     *
     * @param closure changelog extension (ChangelogExtension)
     */
    fun changelog(closure: Closure<ChangeLogExtension>) {
        ConfigureUtil.configure(closure, changelog)
    }

    /**
     * Changelog extenions with configuration for Java/Kotlin
     *
     * @param action changelog extension (ChangelogExtension)
     */
    fun changelog(action: Action<in ChangeLogExtension>) {
        action.execute(changelog)
    }
}