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
import com.intershop.release.version.VersionType
import groovy.util.logging.Slf4j

@Slf4j
class ReleaseFilter extends AbstractBranchFilter {

    private final static threeDigitsFilter = '(\\d+\\.\\d+\\.\\d+)'
    private final static fourDigitsFilter = '(\\d+\\.\\d+\\.\\d+.\\d+)'
    // regex pattern will be created with some predefined variables
    private def regexPattern

    ReleaseFilter(PrefixConfig prefixConfig, Version sourceVersion) {
        String patternString = "^${prefixConfig.getPrefix(BranchType.tag)}${prefixConfig.getTagPrefixSeperator() ? prefixConfig.getTagPrefixSeperator().replace("/", "\\/") : prefixConfig.getPrefixSeperator().replace("/", "\\/")}"
        if(sourceVersion.getNormalVersion().getVersionType() == VersionType.threeDigits) {
            patternString += threeDigitsFilter
        } else {
            patternString += fourDigitsFilter
        }

        if(sourceVersion.branchMetadata) {
            patternString += "-${sourceVersion.branchMetadata}"
        }
        patternString += "(${Version.METADATA_SEPARATOR}(\\w+\\.?\\d+))?"

        log.debug('Release filter is {}', patternString)
        regexPattern = /${patternString}/
    }

    @Override
    public String getVersionStr(String test) {
        def m = test =~ regexPattern
        if(m.matches() && m.count == 1 && m[0].size() > 0) {
            return test.substring(test.indexOf((m[0][1])))
        }

        return ''
    }
}

