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

import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.services.file.FileLocalService
import com.intershop.gradle.scm.services.file.FileVersionService
import com.intershop.gradle.scm.services.git.GitLocalService
import com.intershop.gradle.scm.services.git.GitRemoteService
import com.intershop.gradle.scm.services.git.GitVersionService
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.ScmType
import com.intershop.gradle.scm.utils.getValue
import com.intershop.gradle.scm.utils.setValue
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.VersionType
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

/**
 * <p>This is the extension object for the Intershop version plugin.</p>
 *
 * <pre>
 * {@code
 * scmVersion {
 *     type = 'threeDigits' | 'fourDigits' (default: 'fourDigits')
 *
 *     dryRun = true | false (default: false)
 * }
 * }
 * </pre>
 */
abstract class VersionExtension @Inject constructor(val scmExtension: ScmExtension) {

    companion object {

        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        /**
         * Environment variable for increment.
         */
        const val INCREMENT = "INCREMENT"

        /**
         * Environment variable to enable continuous release.
         */
        const val CONTINUOUSRELEASE = "CONTINUOUSRELEASE"

        /**
         * Static version extension.
         */
        const val SCMVERSIONEXT = "SCMVERSIONEXT"

        // Directory in the build directory for static version
        private const val SCMVERSIONDIR = "scmversion"
        // file name for file with a static version
        private const val STATICVERSIONFILE = "static.version"
    }

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

    private val typeProperty: Property<String> = objectFactory.property(String::class.java)
    private val incrementProperty: Property<String> = objectFactory.property(String::class.java)
    private val initialVersionProperty: Property<String> = objectFactory.property(String::class.java)
    private val versionBranchProperty: Property<String> = objectFactory.property(String::class.java)
    private val patternDigitsProperty: Property<Int> = objectFactory.property(Int::class.java)
    private val defaultBuildMetadataProperty: Property<String> = objectFactory.property(String::class.java)
    private val useBuildExtensionProperty: Property<Boolean> = objectFactory.property(Boolean::class.java)
    private val majorVersionOnlyProperty: Property<Boolean> = objectFactory.property(Boolean::class.java)
    private val disableSCMProperty: Property<Boolean> = objectFactory.property(Boolean::class.java)
    private val continuousReleaseProperty: Property<Boolean> = objectFactory.property(Boolean::class.java)
    private val continuousReleaseBranchesProperty: ListProperty<String> = objectFactory.listProperty(String::class.java)

    private val staticVersionProperty: Property<String> = objectFactory.property(String::class.java)

    init {
        typeProperty.convention("threeDigits")
        incrementProperty.convention(
                (System.getProperty(INCREMENT) ?: System.getenv(INCREMENT) ?: "").toString().trim())
        initialVersionProperty.convention("")

        continuousReleaseProperty.convention(
                (System.getProperty(CONTINUOUSRELEASE) ?: System.getenv(CONTINUOUSRELEASE) ?: "")
                        .toString().trim().toBoolean())

        versionBranchProperty.convention(BranchType.TAG.toString())
        defaultBuildMetadataProperty.convention("")
        useBuildExtensionProperty.convention(false)
        majorVersionOnlyProperty.convention(true)
        disableSCMProperty.convention(false)

        val staticVersionFile = projectLayout.buildDirectory.
                file("${SCMVERSIONDIR}/${STATICVERSIONFILE}").get().asFile

        patternDigitsProperty.set(2)

        if(staticVersionFile.exists()) {
            staticVersionProperty.set(staticVersionFile.readText().trim())
        }
    }

    val versionService: ScmVersionService by lazy {
        if (scmExtension.scmType == ScmType.GIT) {
            GitVersionService(this, GitRemoteService(scmExtension.localService as GitLocalService, scmExtension.user, scmExtension.key ))
        } else {
            FileVersionService(this, scmExtension.localService as FileLocalService)
        }
    }

    /**
     * <p>Digits of the version number</p>
     *
     * <p>Possible values: 'fourDigits' (Intershop), 'threeDigits' (semver)</p>
     */
    var type: String by typeProperty

    /**
     * Calculates the VersionType from a string in the configuration.
     *
     * @property versionType type of the version - three or four digits
     */
    val versionType: VersionType
        get() {
            try {
                return VersionType.valueOf(typeProperty.get())
            } catch (ex: IllegalArgumentException ) {
                log.error("Version type is wrong (set: {}) (exception: {}), possible values: threeDigits, fourDigits.", typeProperty.get(), ex.message)
            }

            log.warn("Default value 'fourDigits' is used for versionType.")
            return VersionType.fourDigits
        }

    /**
     * Returns an extension if disableSCM is true
     * @property versionExt Value of SCMVERSIONEXT_ENV or SCMVERSIONEXT_PRJ
     */
    val versionExt: String
        get() {
            return (System.getProperty(SCMVERSIONEXT) ?: System.getenv(SCMVERSIONEXT) ?: "").toString().trim()
        }

    /**
     * Calculates the VersionBranch from a string of the configuration.
     *
     * @property versionBranchType version branch configuration - tag or branch
     */
    val versionBranchType: BranchType
        get() {
            try {
                val bt = BranchType.valueOf( versionBranchProperty.get())
                if(bt == BranchType.TAG || bt == BranchType.BRANCH) {
                    return bt
                }
            } catch (ex: IllegalArgumentException){
                log.error("Parameter for versionBranch is wrong (set: {}) (exception: {}), possible values: {}, {}.",
                        versionBranchProperty.get(), ex.message,
                        BranchType.TAG.toString(), BranchType.BRANCH.toString())
            }

            log.warn("Default value 'tag' is used for versionBranch.")
            return BranchType.TAG
        }

    /**
     * Caluculates the version from the SCM
     *
     * @property version version string
     */
    val version: String by lazy {
        this.versionService.version
    }

    /**
     * Returns the previous version.
     * @property  information with Version and Tag information
     */
    val previousVersion: String? by lazy {
        if(this.versionService.previousVersion != null) {
            this.versionService.previousVersion.toString()
        } else {
            null
        }
    }

    /**
     * Returns the branch name.
     * @return short branch name
     */
    val branchName: String by lazy {
        var tempBranchName = this.versionService.localService.branchName
        if(tempBranchName.lastIndexOf("/") + 1 < tempBranchName.length)  {
            tempBranchName = tempBranchName.substring(tempBranchName.lastIndexOf('/') + 1)
        }
        tempBranchName
    }


    /**
     * Returns the previous version tag.
     * @param target
     * @return information with Version and Tag information
     */
    fun getPreviousVersionTag(target: String = ""): VersionTag {
        return this.versionService.getPreviousVersionTag(target)
    }

    /**
     * <p>The name of the digit which will be increased.
     * If this value is null, it is always an increment
     * on the latest position and for the trunk or master
     * the previous version.</p>
     *
     * <p>Possible values: MAJOR, MINOR, PATCH, HOTFIX</p>
     *
     * <p>Can be configured/overwritten with environment variable INCREMENT;
     * java environment INCREMENT or project variable increment</p>
     */
    var increment: String by incrementProperty

    /**
     * Initial version is used, if the calculation from the
     * SCM is not possible.
     */
    var initialVersion: String by initialVersionProperty

    /**
     * This kind of branch is used for the version calculation.
     * Possible values: tag, branch
     */
    var versionBranch: String by versionBranchProperty

    /**
     * number of digits for filtering
     * available versions on version branches
     */
    var patternDigits: Int by patternDigitsProperty

    /**
     * meta data setting for feature branches
     */
    var defaultBuildMetadata: String by defaultBuildMetadataProperty

    /**
     * Build extension will be removed for
     * SNAPSHOT extensions if this property is false
     */
    var useBuildExtension: Boolean by useBuildExtensionProperty

    /** This property affects only GIT based repositories.
     *
     * If this property is true and branchWithVersion is false,
     * the version is always only the major version.
     * if the increment property is always configure for MAJOR
     * the version will be increased.
     */
    var majorVersionOnly: Boolean by majorVersionOnlyProperty

    /**
     * If this property is true, the initial
     * version is always used and the SCM usage
     * is disabled.
     * The environment variable 'SCMVERSIONEXT' or
     * the project variable 'scmVersionExt'
     * will be used on the CI server for special
     * extensions. If this values is
     * SNAPSHOT - SNAPSHOT will be added to the version
     * RELEASE - without any extension
     * If no value is specified a time stamp
     * will be added.
     * On the local developer machine LOCAL will
     * be added to the version.
     */
    var disableSCM: Boolean by disableSCMProperty

    /**
     * For continuous releases it is helpful to use an
     * version extension with an relation to the
     * source control.
     * This property can be configured for this cases
     * to true.
     */
    var continuousRelease: Boolean by continuousReleaseProperty

    /**
     * In combination with continuousRelease it should
     * be possible to specify the branches for this
     * kind of version extension.
     * Continuous releases will be also used for the master or trunk.
     * If you want extend the list of branches, it is possible to
     * extend the list.
     */
    var continuousReleaseBranches: List<String> by continuousReleaseBranchesProperty

}