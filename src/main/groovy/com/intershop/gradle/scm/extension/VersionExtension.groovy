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

import com.intershop.gradle.scm.builder.ScmBuilder
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.Version
import com.intershop.release.version.VersionType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project
/**
 * <p>This is the extension object for the Intershop version plugin.</p>
 *
 * <pre>
 * {@code
 * scmVersion {
 *     type = 'threeDigits' | 'fourDigits' (default: 'fourDigits')
 *
 *     dryRun = true | false (default: false)
 *
 *     runOnCI = true | false (default: false)
 * }
 * }
 * </pre>
 */
@CompileStatic
@Slf4j
class VersionExtension extends AbstractExtension {

    // Directory in the build directory for static version
    private final static String SCMVERSIONDIR = 'scmversion'
    // file name for file with a static version
    private final static String STATICVERSIONFILE = 'static.version'

    // settings for environment configuration
    // dry run
    public final static String DRYRUN_ENV = 'DRYRUN'
    public final static String DRYRUN_PRJ = 'dryRun'

    // run on CI server
    public final static String RUNONCI_ENV = 'RUNONCI'
    public final static String RUNONCI_PRJ = 'runOnCI'

    // increment
    public final static String INCDIGIT_ENV = 'INCREMENT'
    public final static String INCDIGIT_PRJ = 'increment'

    //SCMVERSIONEXT
    public final static String SCMVERSIONEXT_ENV = 'SCMVERSIONEXT'
    public final static String SCMVERSIONEXT_PRJ = 'scmVersionExt'

    // variable for version which is used if the
    // system is offline without any connection to
    // the remote repository
    private final String offlineVersion

    // variable for a project specific static version
    // this version is taken from a file and without
    // any connection to the version control system
    private final String staticVersion

    // version service
    private ScmVersionService versionService

    // cached strings
    private String internalVersion
    private String internalPreviousVersion
    private String internalBranchName

    /**
     * <p>Digits of the version number</p>
     *
     * <p>Possible values: 'fourDigits' (Intershop), 'threeDigits' (semver)</p>
     */
    String type

    /**
     * <p>Run tasks without changes on the SCM</p>
     *
     * <p>Can be configured/overwritten with environment variable DRYRUN;
     * java environment DRYRUN or project variable dryRun</p>
     */
    boolean dryRun

    /**
     * <p>Configuration for the execution on the CI server</p>
     *
     * <p>Can be configured/overwritten with environment variable RUNONCI;
     * java environment RUNONCI or project variable runOnCI</p>
     */
    boolean runOnCI

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
    String increment

    /**
     * Initial version is used, if the calculation from the
     * SCM is not possible.
     */
    String initialVersion

    /**
     * This kind of branch is used for the version calculation.
     * Possible values: tag, branch
     */
    String versionBranch

    /**
     * number of digits for filtering
     * available versions on version branches
     */
    int patternDigits = 2

    /**
     * meta data setting for feature branches
     */
    String defaultBuildMetadata = ''

    /**
     * Build extension will be removed for
     * SNAPSHOT extensions if this property is false
     */
    boolean useBuildExtension = false

    /**
     * This property affects only GIT based repositories.
     *
     * If this property is true the version of feature branches
     * is calculated from any tag on the branch.
     * Therefore it is not necessary to specify
     * a version in the branch name.
     */
    boolean branchWithVersion = true

    /** This property affects only GIT based repositories.
     *
     * If this property is true and branchWithVersion is false,
     * the version is always only the major version.
     * if the increment property is always configure for MAJOR
     * the version will be increased.
     */
    boolean majorVersionOnly = true

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
    boolean disableSCM = false

    /**
     * For continuous releases it is helpful to use an
     * version extension with an relation to the
     * source control.
     * This property can be configured for this cases
     * to true.
     */
    boolean continuousRelease = false

    /**
     * In combination with continuousRelease it should
     * be possible to specify the branches for this
     * kind of version extension.
     * Continuous releases will be also used for the master or trunk.
     * If you want extend the list of branches, it is possible to
     * extend the list.
     */
    List<String> continuousReleaseBranches = []

    /**
     * Initialize this extension and set default values
     * If environment values are set for some keys this
     * values will be used.
     *
     * @param project
     */
    VersionExtension(Project project) {
        super(project)

        // init default value for dry run
        if(! dryRun) {
            dryRun = Boolean.parseBoolean(getVariable(DRYRUN_ENV, DRYRUN_PRJ, 'false'))
            if(dryRun) {
                log.warn('DryRun is enabled!')
            }
        }

        // init default value for runOnCI
        if(! runOnCI) {
            runOnCI = Boolean.parseBoolean(getVariable(RUNONCI_ENV, RUNONCI_PRJ, 'false'))
            if(runOnCI) {
                log.warn('All tasks will be executed on a CI build environment.')
            }
        }

        // initial setting for type
        // default is the Intershop product version
        if(! type) {
            type = 'threeDigits'
        }

        // default value depends on the SCM
        if(! increment) {
            increment = getVariable(INCDIGIT_ENV, INCDIGIT_PRJ, '')
        }

        // default branch for version calculation
        if(!versionBranch) {
            versionBranch = BranchType.tag.toString()
        }

        // init offline version
        if(project.hasProperty('offlineVersion')) {
            offlineVersion = project.property('offlineVersion')
        }

        File staticVersionFile = new File(project.buildDir, "${SCMVERSIONDIR}/${STATICVERSIONFILE}")
        if(project.hasProperty('staticVersion')) {
            String sv = project.property('staticVersion').toString()
            if(sv) {
                staticVersionFile.getParentFile().mkdirs()
                staticVersionFile.setText(sv)
            } else {
                staticVersionFile.delete()
            }
        }
        if(staticVersionFile.exists()) {
            staticVersion = staticVersionFile.text.trim()
        }
    }

    /**
     * Calculates the VersionType from a string in the configuration.
     *
     * @return type of the version - three or four digits
     */
    VersionType getVersionType() {
        try {
            return Enum.valueOf(VersionType, getType())
        } catch (IllegalArgumentException ex) {
            log.error('Version type is wrong (set: {}) (exception: {}), possible values: threeDigits, fourDigits.', getType(), ex.getMessage())
        }

        log.warn("'fourDigits' is used for versionType.")
        return VersionType.fourDigits
    }

    /**
     * Returns an extension if disableSCM is true
     * @return Value of SCMVERSIONEXT_ENV or SCMVERSIONEXT_PRJ
     */
    String getVersionExt() {
        String scmext = getVariable(SCMVERSIONEXT_ENV, SCMVERSIONEXT_PRJ, '')
        return scmext.toUpperCase()
    }

    /**
     * Calculates the VersionBranch from a string of the configuration.
     *
     * @return version branch configuration - tag or branch
     */
    BranchType getVersionBranchType() {
        try {
            BranchType bt = Enum.valueOf(BranchType, getVersionBranch())
            if(bt == BranchType.tag || bt == BranchType.branch) {
                return bt
            }
        } catch (IllegalArgumentException ex){
            log.error('Parameter for versionBranch is wrong (set: {}) (exception: {}), possible values: {}, {}.', getVersionBranch(), ex.getMessage(), BranchType.tag.toString(), BranchType.branch.toString())
        }

        log.warn("'tag' is used for versionBranch.")
        return BranchType.tag
    }

    /**
     * Caluculates the version from the SCM
     *
     * @return version string
     */
    String getVersion() {
        if(offlineVersion) {
            return offlineVersion
        }
        if(staticVersion) {
            return staticVersion
        }
        if (! internalVersion) {
            internalVersion = this.getVersionService().version
        }
        return internalVersion
    }

    /**
     * Returns the previous version.
     * @param target
     * @return information with Version and Tag information
     */
    String getPreviousVersion() {
        if(! internalPreviousVersion) {
            Version prevVersion = this.getVersionService().getPreviousVersion()
            internalPreviousVersion = prevVersion ? prevVersion.toString() : ''
        }
        return internalPreviousVersion
    }

    /**
     * Returns the branch name
     * @return short branch name
     */
    String getBranchName() {
        if(! internalBranchName) {
            String bname = this.getVersionService().getLocalService().branchName
            internalBranchName = bname.lastIndexOf('/') + 1 < bname.length() ? bname.substring(bname.lastIndexOf('/') + 1) : bname
        }
        return internalBranchName
    }

    /**
     * Returns the previous version tag.
     * @param target
     * @return information with Version and Tag information
     */
    VersionTag getPreviousVersionTag(String target = '') {
        this.getVersionService().getPreviousVersionTag(target)
    }

    /**
     * Holds the version service
     *
     * @return version service based on the SCM
     */
    ScmVersionService getVersionService() {
        if(! versionService) {
            versionService = ScmBuilder.getScmVersionService(project, this)
        }
        return versionService
    }

    ScmVersionService updateVersionService() {
        versionService = ScmBuilder.getScmVersionService(project, this)

        internalVersion = null
        internalBranchName = null
        internalPreviousVersion = null

        return versionService
    }
}
