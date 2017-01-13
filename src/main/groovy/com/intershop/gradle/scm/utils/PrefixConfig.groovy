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


package com.intershop.gradle.scm.utils

import groovy.transform.CompileStatic
/**
 * This is the configuration class for the necessary prefixes
 * on special branches, so that it is possible to identify the
 * relevant branches and tags for version calculation.
 */
@CompileStatic
class PrefixConfig {

    /**
     * Search pattern for branches with version information.
     */
    protected final static String featureBranchPatternSuffix = "(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)-(.+)"

    /**
     * Prefix for stabilization branches
     */
    String stabilizationPrefix

    /**
     * Prefix for feature branches
     */
    String featurePrefix

    /**
     * Prefix for release tags
     */
    String tagPrefix

    /**
     * <p>Constructs a prefix configuration with
     * default values.</p>
     * <p><b>stabilization branche prefix</b> - SB</p>
     * <p><b>feature branche prefix</b> - FB</p>
     * <p><b>release tag prefix</b> - RELEASE</p>
     */
    PrefixConfig() {
        stabilizationPrefix = 'SB'
        featurePrefix = 'FB'
        tagPrefix = 'RELEASE'
    }

    /**
     * <p>Constructs a prefix configuration with
     * parameters.</p>
     *
     * @param stabilizationPrefix prefix for stabilization branches
     * @param featurePrefix prefix for feature branches
     * @param tagPrefix prefix for release tags
     */
    PrefixConfig(String stabilizationPrefix, String featurePrefix, String tagPrefix) {
        this.stabilizationPrefix = stabilizationPrefix
        this.featurePrefix = featurePrefix
        this.tagPrefix = tagPrefix
    }

    /**
     * Separator between prefix and version.
     */
    String prefixSeperator = '_'

    /**
     * Separator between prefix and version for branches.
     */
    String branchPrefixSeperator = null

    /**
     * Separator between prefix and version for tags.
     */
    String tagPrefixSeperator = null

    /**
     * Creates a search pattern for feature branches.
     *
     * @return Search pattern for feature branches.
     */
    public String getFeatureBranchPattern() {
        if(branchPrefixSeperator) {
            return "${featurePrefix}${branchPrefixSeperator}${featureBranchPatternSuffix}"
        }
        return "${featurePrefix}${prefixSeperator}${featureBranchPatternSuffix}"
    }

    /**
     * Checks the prefix. An empty prefix for stabilization branches
     * is not allowed.
     *
     * @return validated prefix for stabilization branches
     */
    public String getStabilizationPrefix() {
        return validatePrefix('stabilization branches', stabilizationPrefix)
    }

    /**
     * Checks the prefix. An empty prefix for feature branches
     * is not allowed.
     *
     * @return validated prefix for feature branches
     */
    public String getFeaturePrefix() {
        return validatePrefix('feature branches', featurePrefix)
    }

    /**
     * Checks the prefix. An empty prefix for release tags
     * is not allowed.
     *
     * @return validated prefix for release tags
     */
    public String getTagPrefix() {
        return validatePrefix('release tags', tagPrefix)
    }

    /**
     * Returns the prefix for the special branch.
     *
     * @param type branch type
     * @return the prefix for the specified branch type
     */
    public String getPrefix(BranchType type) {
        switch (type) {
            case BranchType.branch:
                return stabilizationPrefix
                break
            case BranchType.featureBranch:
                return featurePrefix
                break
            case BranchType.tag:
                return tagPrefix
                break
        }
    }

    /**
     * Returns the branch type for the specified prefix.
     *
     * @param prefix string prefix
     * @return the branch type for the specified prefix
     * @throws com.intershop.gradle.scm.utils.ScmException if the prefix is not configured.
     */
    public BranchType getBranchType(String prefix) {
        if(prefix == stabilizationPrefix) {
            return BranchType.branch
        }
        if(prefix == featurePrefix) {
            return BranchType.featureBranch
        }
        if(prefix == tagPrefix) {
            return BranchType.tag
        }
        throw new ScmException('Prefix is not specified!')
    }

    private String validatePrefix(String type, String prefix) {
        if(! prefix) {
            throw new IllegalArgumentException("The setting for ${type} is eampty!")
        }
        return prefix.trim()
    }
}
