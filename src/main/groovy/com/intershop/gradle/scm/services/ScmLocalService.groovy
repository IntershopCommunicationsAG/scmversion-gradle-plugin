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

package com.intershop.gradle.scm.services

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.PrefixConfig
import com.intershop.gradle.scm.utils.ScmType

import groovy.transform.CompileStatic

/**
 * This is the container of all information from an existing
 * working copy without any access to the remote location of the project.
 */
@CompileStatic
abstract class ScmLocalService {

    /**
     * The base branch type of the current working copy.
     */
    protected BranchType branchType = null

    /**
     * The base (stabilization) branch name of the current working copy
     */
    protected String branchName = ''

    /**
     * The base feature branch name of the current working copy
     */
    protected String featureBranchName = ''

    /**
     * This is true, if the local working copy changed.
     */
    boolean changed = false

    /**
     * The directory of the working copy.
     */
    protected final File projectDir

    /**
     * This is true, if the feature branch contains a version.
     */
    boolean withVersion = false

    /**
     * The configuration of all prefixes.
     */
    public final PrefixConfig prefixes

    /**
     * Store the type of the SCM
     */
    public final ScmType type

    /**
     * This constructs a SCM info service. It contains all necessary information from
     * the working copy without remote access to the SCM. It must be implemented for
     * supported SCMs.
     *
     * @param projectDir
     * @param prefixes
     */
    ScmLocalService(File projectDir, ScmExtension scmExtension) {
        this.projectDir = projectDir
        this.prefixes = scmExtension.getPrefixes()
        this.type = scmExtension.getScmType()
    }

    /**
     * The directory of the working copy (read only).
     *
     * @return directory of the working copy.
     */
    File getProjectDir() {
        return projectDir
    }

    /**
     * The branch type on the SCM of the working copy (read only).
     *
     * @return true if the working copy was changed.
     */
    BranchType getBranchType() {
        return branchType
    }

    /**
     * The (stabilization) branch name on the SCM of the working copy (read only).
     *
     * @return branch name of the working copy.
     */
    String getBranchName() {
        return branchName
    }

    /**
     * The feature branch name on the SCM of the working copy (read only).
     *
     * @return branch name of the working copy.
     */
    String getFeatureBranchName() {
        return featureBranchName
    }

    /**
     * Parses the input to see if a version is part of the string.
     *
     * @param branch complete branch name without prefix
     * @return the branch name without version
     */
    void setFeatureBranchName(String branch) {
        def branchCheck = branch =~ /^\d+(\.\d+)?(\.\d+)?(\.\d+)?(-.*)?/
        if (branchCheck.matches() && branchCheck.groupCount() > 3) {
            featureBranchName = ((branchCheck[0] as List)[(branchCheck[0] as List).size() - 1].toString())
        } else {
            featureBranchName = branch
        }
        withVersion = featureBranchName != branch
    }

    /**
     * If the feature branch contains a valid version
     * ist returns true otherwise false.
     *
     * @return true if feature branch contains version
     */
    boolean isBranchWithVersion() {
        return withVersion
    }
    /**
     * The information if the working copy was changed before (read only).
     *
     * @return true if the working copy was changed.
     */
    boolean isChanged() {
        return changed
    }

    /**
     * It returns the remote url, calculated from the properties of the working copy (read only).
     *
     * @return remote url
     */
    abstract String getRemoteUrl()

    /**
     * The revision id from the working copy (read only).
     *
     * @return revision id
     */
    abstract String getRevID()
}
