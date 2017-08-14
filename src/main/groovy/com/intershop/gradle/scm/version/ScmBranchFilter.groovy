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
package com.intershop.gradle.scm.version

import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.PrefixConfig
import com.intershop.release.version.Version
import groovy.util.logging.Slf4j

/**
 * This is a filter object to find the correct branch with version information.
 */
@Slf4j
class ScmBranchFilter extends AbstractBranchFilter {

    /**
     * Search pattern for all branches and tags with version information.
     */
    private static final String versionregex = '(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)'

    // regex pattern will be created with some predefined variables
    private def regexPattern
    // feature branch key
    private String featureBranch = ''
    // prefix configuration
    private PrefixConfig prefixes

    /**
     * Contstructs thre version branch filter.
     *
     * @param versionBranchtype which version branch is used for version calculation
     * @param prefixes prefix configuration
     * @param branchFilterName this can be the base for filtering (default: '')
     * @param featureBranch feature branch key (default: '')
     * @param patternDigits number of digits for the filter (default: 2)
     */
    ScmBranchFilter(BranchType versionBranchtype, PrefixConfig prefixes, String branchFilterName = '', BranchType branchFilterType , String featureBranch = '', int patternDigits = 2) {
        String[] vdata = new String[4]
        this.prefixes = prefixes

        log.debug("Create filter for type:${versionBranchtype}, name:${branchFilterName}, featurebranch:${featureBranch}, pattern:${patternDigits}")

        if(branchFilterName) {
            String branchFilterNamePattern = ''

            branchFilterNamePattern += branchFilterType == BranchType.branch && prefixes.getBranchPrefixSeperator() ? prefixes.getBranchPrefixSeperator() : ''
            branchFilterNamePattern += branchFilterType == BranchType.tag && prefixes.getTagPrefixSeperator() ? prefixes.getTagPrefixSeperator() : ''
            branchFilterNamePattern += !prefixes.getTagPrefixSeperator() && !prefixes.getBranchPrefixSeperator() ? prefixes.getPrefixSeperator() : ''

            branchFilterNamePattern += versionregex
            def m = branchFilterName =~ /${branchFilterNamePattern}/

            if(m.getCount()) {
                String tv = m[0][1]
                vdata = tv.split('\\.')
            }
        }

        List dp = ['\\d+', '(\\.\\d+)?', '(\\.\\d+)?', '(\\.\\d+)?' ]
        for(int i = 0; i < Math.min(patternDigits, vdata.length); i++) {
            if(vdata[i]) { dp[i] = "${i==0 ? '' : '.'}${vdata[i]}" }
        }

        String patternString = "^${this.prefixes.getPrefix(versionBranchtype)}"

        if((versionBranchtype == BranchType.branch || versionBranchtype == BranchType.featureBranch || versionBranchtype == BranchType.hotfixbBranch || versionBranchtype == BranchType.bugfixBranch) && prefixes.getBranchPrefixSeperator()) {
            patternString += "${prefixes.getBranchPrefixSeperator()}("
        } else if(versionBranchtype == BranchType.tag && prefixes.getTagPrefixSeperator()) {
            patternString += "${prefixes.getTagPrefixSeperator()}("
        } else {
            patternString += "${prefixes.getPrefixSeperator()}("
        }

        patternString += "${dp[0]}${dp[1]}${dp[2]}${dp[3]}"

        switch (versionBranchtype) {
            case BranchType.branch:
                patternString += '$)'
                break
            case BranchType.featureBranch:
                patternString += ")"
                break
            case BranchType.hotfixbBranch:
                patternString += ")"
                break
            case BranchType.bugfixBranch:
                patternString += ")"
                break
            case BranchType.tag:
                patternString += ")"
                break
        }

        if(featureBranch && (versionBranchtype == BranchType.featureBranch || versionBranchtype == BranchType.bugfixBranch || versionBranchtype == BranchType.hotfixbBranch || versionBranchtype == BranchType.tag)) {
            patternString += "${Version.METADATA_SEPARATOR}${featureBranch}"
        }
        if(! featureBranch && versionBranchtype == BranchType.tag) {
            patternString += "(${Version.METADATA_SEPARATOR}.*)?"
        }
        if(versionBranchtype == BranchType.tag) {
            patternString += "(${Version.METADATA_SEPARATOR}(\\w+\\.?\\d+))?"
        }

        log.debug('Branch filter is {}', patternString)

        regexPattern = /${patternString}/
    }

    /**
     * Analyses the input parameter with the created filter and returns a valid
     * version string or an empty string, if the input is not valid or does not match.
     *
     * @param test input string
     * @return a valid version string or an empty string
     */
    public String getVersionStr(String test) {
        def m = test =~ regexPattern

        if(m.matches() && m.count == 1 && m[0].size() > 0) {
            return test.substring(test.indexOf((m[0][1])))
        }

        return ''
    }
}
