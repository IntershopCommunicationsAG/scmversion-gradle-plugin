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
package com.intershop.gradle.scm.version

import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.IPrefixConfig
import com.intershop.release.version.Version
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.min

/**
 * Filter for version calculation from branches and tags.
 *
 * @constructor creates the filter
 * @param prefixes  prefix configurtion
 * @param versionBranchtype version from branch or tag
 * @param branchFilterName branch name for filtering
 * @param branchFilterType branch type for filtering
 * @param featureBranch feature branch name if available
 * @param patternDigits numer of pattern digits for filtering
 */
class ScmBranchFilter(val prefixes: IPrefixConfig,
                      private var versionBranchtype: BranchType? = null,
                      private var branchFilterName: String = "",
                      private var branchFilterType: BranchType? = null,
                      private var featureBranch: String = "",
                      private var patternDigits: Int = 2): AbstractBranchFilter() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        /**
         * Search pattern for all branches and tags with version information.
         */
        const val versionregex = "(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)"

        /**
         * List of all branch types.
         */
        val allBranches = listOf(BranchType.BRANCH, BranchType.FEATUREBRANCH,
                BranchType.BUGFIXBRANCH, BranchType.HOTFIXBBRANCH)

        /**
         * List of specail branch types and tag.
         */
        val branchesAndTag = listOf(BranchType.TAG, BranchType.FEATUREBRANCH,
                BranchType.BUGFIXBRANCH, BranchType.HOTFIXBBRANCH)
    }

    private val regex: Regex by lazy {
        var vdata = arrayOf("","","","")
        val dp = mutableListOf("\\d+", "(\\.\\d+)?", "(\\.\\d+)?", "(\\.\\d+)?")

        var patternString = "^${this.prefixes.getPrefix(versionBranchtype ?: BranchType.TAG)}"

        if(versionBranchtype == null && branchFilterName == "" && branchFilterType == null && featureBranch == "") {
            log.debug("Create filter for tags.")
            patternString += "${prefixes.prefixSeperator.replace("/", "\\/")}("
            patternString += "${dp[0]}${dp[1]}${dp[2]}${dp[3]}"
            patternString += ")(${Version.METADATA_SEPARATOR}(\\w+\\.?\\d+))?"

        } else {

            log.debug("Create filter for type:${versionBranchtype}, name:${branchFilterName}, " +
                    "featurebranch:${featureBranch}, pattern:${patternDigits}")

            var branchFilterNamePattern = ".*"

            if(branchFilterType == BranchType.BRANCH && prefixes.branchPrefixSeperator != null) {
                branchFilterNamePattern += prefixes.branchPrefixSeperator
            }
            if(branchFilterType == BranchType.TAG && prefixes.tagPrefixSeperator != null) {
                branchFilterNamePattern += prefixes.tagPrefixSeperator
            }
            if(prefixes.tagPrefixSeperator == null && prefixes.branchPrefixSeperator == null) {
                branchFilterNamePattern += prefixes.prefixSeperator
            }
            branchFilterNamePattern = branchFilterNamePattern.replace("/", "\\/")

            branchFilterNamePattern += versionregex

            val branchRegex = Regex(branchFilterNamePattern)
            val branchMatchResult: MatchResult? = branchRegex.matchEntire(branchFilterName)

            if(branchMatchResult?.groupValues != null && branchMatchResult.groupValues.isNotEmpty()) {
                val tv = branchMatchResult.groupValues[1]
                vdata = tv.split(".").toTypedArray()
            }

            for(i in 0 until min(patternDigits, vdata.size)) {
                if(vdata[i].isNotEmpty()) {
                    dp[i] =  if(i == 0) { vdata[i] } else { ".${vdata[i]}" }
                }
            }

            patternString = "^${this.prefixes.getPrefix(versionBranchtype!!)}"

            val tempPrefix = if(prefixes.branchPrefixSeperator != null && allBranches.contains(versionBranchtype!!)) {
                                    prefixes.branchPrefixSeperator
                                } else if(versionBranchtype == BranchType.TAG && prefixes.tagPrefixSeperator != null) {
                                    prefixes.tagPrefixSeperator
                                } else {
                                    prefixes.prefixSeperator
                                }

            patternString += "${tempPrefix!!.replace("/", "\\/")}("
            patternString += "${dp[0]}${dp[1]}${dp[2]}${dp[3]}"

            patternString += when (versionBranchtype) {
                BranchType.BRANCH -> "$)"
                else              -> ")"
            }

            if(featureBranch.isNotEmpty() && branchesAndTag.contains(versionBranchtype!!)) {
                patternString += "${Version.METADATA_SEPARATOR}${featureBranch}"
            }
            if(featureBranch.isEmpty() && versionBranchtype == BranchType.TAG) {
                patternString += "(${Version.METADATA_SEPARATOR}.*)?"
            }
            if(versionBranchtype == BranchType.TAG) {
                patternString += "(${Version.METADATA_SEPARATOR}(\\w+\\.?\\d+))?"
            }

            log.debug("Branch filter is {}", patternString)
        }
        Regex(patternString)
    }

    /**
     * Get version string from branch.
     * @param branch name of the branch/tag
     * @return version string
     */
    override fun getVersionStr(branch: String): String {

        val matchResult: MatchResult? = regex.matchEntire(branch)
        if(matchResult?.groupValues != null && matchResult.groupValues.isNotEmpty()) {
            return branch.substring(branch.indexOf(matchResult.groupValues[1]))
        }
        return ""
    }
}
