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
package com.intershop.gradle.scm.version

import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.IPrefixConfig
import com.intershop.release.version.Version
import com.intershop.release.version.VersionType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class ReleaseFilter(private val prefixConfig: IPrefixConfig,
                         private val sourceVersion: Version): AbstractBranchFilter() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        const val threeDigitsFilter = "(\\d+\\.\\d+\\.\\d+)"
        const val fourDigitsFilter = "(\\d+\\.\\d+\\.\\d+.\\d+)"
    }

    private val regex: Regex by lazy {
        val prefixSep = (if(prefixConfig.tagPrefixSeperator != null) {
            prefixConfig.tagPrefixSeperator
        } else {
            prefixConfig.prefixSeperator
        })?.replace("/", "\\/")

        var patternString = "^${prefixConfig.getPrefix(BranchType.TAG)}${prefixSep}"

        patternString += if(sourceVersion.normalVersion.versionType == VersionType.threeDigits) {
            threeDigitsFilter
        } else {
            fourDigitsFilter
        }

        if(sourceVersion.branchMetadata != null && ! sourceVersion.branchMetadata.isEmpty) {
            patternString += "-${sourceVersion.branchMetadata}"
        }
        patternString += "(${Version.METADATA_SEPARATOR}(\\w+\\.?\\d+))?"

        log.debug("Release filter is {}", patternString)

        Regex(patternString)
    }

    override fun getVersionStr(branch: String): String {

        val matchResult: MatchResult? = regex.matchEntire(branch)
        if(matchResult?.groupValues != null && matchResult.groupValues.isNotEmpty()) {
            return branch.substring(branch.indexOf(matchResult.groupValues[1]))
        }
        return ""
    }
}

