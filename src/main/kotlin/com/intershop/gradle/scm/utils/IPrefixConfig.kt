/*
 * Copyright 2020 Intershop Communications AG.
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
package com.intershop.gradle.scm.utils

/**
 * This is the configuration class for the necessary prefixes
 * on special branches, so that it is possible to identify the
 * relevant branches and tags for version calculation.
 */
interface IPrefixConfig {

    companion object {
        /**
         * Search pattern for stabilization branches with version information.
         */
        const val stabilizationBranchPattern = "(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)"
    }

    /**
     * Prefix for stabilization branches.
     *
     * @property stabilizationPrefix
     */
    var stabilizationPrefix: String

    /**
     * Prefix for feature branches.
     *
     * @property featurePrefix
     */
    var featurePrefix: String

    /**
     * Prefix for hotfix branches.
     *
     * @property hotfixPrefix
     */
    var hotfixPrefix: String

    /**
     * Prefix for bugfix branches.
     *
     * @property bugfixPrefix
     */
    var bugfixPrefix: String

    /**
     * Prefix for release tags.
     *
     * @property tagPrefix
     */
    var tagPrefix: String

    /**
     * Separator between prefix and version.
     *
     * @property prefixSeperator
     */
    var prefixSeperator: String

    /**
     * Separator between prefix and version for branches.
     *
     * @property branchPrefixSeperator
     */
    var branchPrefixSeperator: String?

    /**
     * Separator between prefix and version for tags.
     *
     * @property tagPrefixSeperator
     */
    var tagPrefixSeperator: String?

    /**
     * Creates a search pattern for feature branches.
     *
     * @property featureBranchPattern Search pattern for feature branches.
     */
    val featureBranchPattern: String
        get() {
            return if(branchPrefixSeperator != null) {
                            "${featurePrefix}${branchPrefixSeperator}(.*)"
                        } else {
                            "${featurePrefix}${prefixSeperator}(.*)"
                        }
        }

    /**
     * Creates a search pattern for hotfix branches.
     *
     * @property hotfixBranchPattern Search pattern for hotfix branches.
     */
    val hotfixBranchPattern : String
        get() {
            if(branchPrefixSeperator != null) {
                return "${hotfixPrefix}${branchPrefixSeperator}(.*)"
            }
            return "${hotfixPrefix}${prefixSeperator}(.*)"
        }

    /**
     * Creates a search pattern for bugfix branches.
     *
     * @property bugfixBranchPattern Search pattern for bugfix branches.
     */
    val bugfixBranchPattern: String
        get() {
            if(branchPrefixSeperator != null) {
                return "${bugfixPrefix}${branchPrefixSeperator}(.*)"
            }
            return "${bugfixPrefix}${prefixSeperator}(.*)"
        }

    /**
     * Creates a search pattern for stabilization branches.
     *
     * @property stabilizationBranchPattern Search pattern for stabilization branches.
     */
    val stabilizationBranchPattern: String
        get() {
            if(branchPrefixSeperator != null) {
                return "${stabilizationPrefix}${branchPrefixSeperator}(.*)"
            }
            return "${stabilizationPrefix}${prefixSeperator}(.*)"
        }

    /**
     * Returns the prefix for the special branch.
     *
     * @param type branch type
     * @return the prefix for the specified branch type
     */
    fun getPrefix(type: BranchType): String {
        return when (type) {
            BranchType.BRANCH -> stabilizationPrefix
            BranchType.FEATUREBRANCH -> featurePrefix
            BranchType.HOTFIXBBRANCH -> hotfixPrefix
            BranchType.BUGFIXBRANCH -> bugfixPrefix
            else -> tagPrefix
        }
    }
}
