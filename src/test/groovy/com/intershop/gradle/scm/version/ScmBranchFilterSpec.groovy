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
import com.intershop.gradle.scm.utils.IPrefixConfig
import com.intershop.gradle.scm.utils.SimplePrefixConfig
import spock.lang.Specification

class ScmBranchFilterSpec extends Specification {

    def 'trunk filter with prefix'() {
        when:
        SimplePrefixConfig prefixes = new SimplePrefixConfig()
        prefixes.stabilizationPrefix = 'enf7ga'
        prefixes.featurePrefix = 'fbenf7ga'
        prefixes.tagPrefix = 'releases'
        prefixes.setPrefixSeperator('-')
        ScmBranchFilter filter =  new ScmBranchFilter(prefixes, BranchType.BRANCH,  '', null, '', 2)

        then:
        versionStr == filter.getVersionStr(branchName)

        where:
        branchName          | versionStr
        'enf7ga-7.4.6'      | '7.4.6'
        'enf7ga-7.4.6.1'    | '7.4.6.1'
        'enf7ga-7.4.6.2'    | '7.4.6.2'
        'enf7ga-7.5'        | '7.5'
        'enf7ga-7.5.0'      | '7.5.0'
        'enf7ga-7.5.1'      | '7.5.1'
        'enf7ga-7.5.2'      | '7.5.2'
        'enf7ga-7.5.2-fb1'  | ''
        'enf7ga-7.5.2-fb2'  | ''
        'branchname'        | ''
    }

    def 'trunk filter with prefix and three digits'() {
        when:
        SimplePrefixConfig prefixes = new SimplePrefixConfig()
        prefixes.stabilizationPrefix = 'enf7ga'
        prefixes.featurePrefix = 'fbenf7ga'
        prefixes.tagPrefix = 'RELEASE'
        prefixes.setBranchPrefixSeperator('-')
        prefixes.setTagPrefixSeperator('_')

        ScmBranchFilter filter =  new ScmBranchFilter( prefixes, BranchType.TAG,'enf7ga-7.4.6', BranchType.BRANCH, '', 3)

        then:
        versionStr == filter.getVersionStr(branchName)

        where:
        branchName          | versionStr
        'RELEASE_7.4.6.1'      | '7.4.6.1'
        'RELEASE_7.4.6.2'      | '7.4.6.2'
        'RELEASE_7.5.5.1'      | ''
    }

    def 'featurebranch with jira issue'() {
        when:
        SimplePrefixConfig prefixes = new SimplePrefixConfig()
        prefixes.stabilizationPrefix = 'SB'
        prefixes.featurePrefix = 'FB'
        prefixes.tagPrefix = 'RELEASES'

        prefixes.setPrefixSeperator('_')
        ScmBranchFilter filter =  new ScmBranchFilter(prefixes, BranchType.FEATUREBRANCH, '', null, 'ISTOOLS-12345', 2)

        then:
        versionStr == filter.getVersionStr(branchName)

        where:
        branchName                  | versionStr
        'FB_10.0-ISTOOLS-12345'     | '10.0-ISTOOLS-12345'
    }

    def 'branch filter with prefix'() {
        when:
        ScmBranchFilter filter =  new ScmBranchFilter(new SimplePrefixConfig(), BranchType.TAG, '', null, '', 2)

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                  | versionStr
        'RELEASE_7.5.3.0'        | '7.5.3.0'
        'RELEASE_7.5.4.0'        | '7.5.4.0'
        'RELEASE_7.5.4.0-rc1'    | '7.5.4.0-rc1'
        'RELEASE_7.5.4.0-fb1-rc1'| '7.5.4.0-fb1-rc1'
    }

    def 'featurebranch filter with prefix'() {
        when:
        ScmBranchFilter filter =  new ScmBranchFilter(new SimplePrefixConfig(), BranchType.TAG, '', BranchType.TAG, 'fb1', 2)

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                  | versionStr
        'RELEASE_7.5.4.0-fb1-rc1'| '7.5.4.0-fb1-rc1'
    }

    def 'branch filter with prefix for feature branch'() {
        when:
        ScmBranchFilter filter =  new ScmBranchFilter( new SimplePrefixConfig(), BranchType.TAG,'', null, 'fb1', 2)

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                  | versionStr
        'RELEASE_7.5.3.0'        | ''
        'RELEASE_7.5.4.0-rc1'    | ''
        'RELEASE_7.5.4.0-fb1-rc1'| '7.5.4.0-fb1-rc1'
        'BRANCH_7.5.4.0-fb1'     | ''
    }

    def 'branch filter for special version for tags'() {
        when:
        ScmBranchFilter filter =  new ScmBranchFilter(new SimplePrefixConfig(), BranchType.TAG, 'SB_10', BranchType.BRANCH, '', 2)

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                     | versionStr
        'RELEASE_10.10.10.1'        | '10.10.10.1'
        'RELEASE_10.10.10.2'        | '10.10.10.2'
        'RELEASE_10.10.10.3'        | '10.10.10.3'
        'RELEASE_10.10.10.4'        | '10.10.10.4'
        'RELEASE_10.10.10.4-rc1'    | '10.10.10.4-rc1'
        'RELEASE_10..10.10.4-rc1'   | ''
        'RELEASE_10.10.10.4-IS-4711-rc1'   | '10.10.10.4-IS-4711-rc1'
    }

    def 'branch filter for wrong branch and for tags'() {
        when:
        ScmBranchFilter filter =  new ScmBranchFilter(new SimplePrefixConfig(), BranchType.TAG, 'SB_10.0', BranchType.BRANCH, '', 1)

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                     | versionStr
        'RELEASE_10.0.0.1'          | '10.0.0.1'
        'RELEASE_10.1.0.0'          | '10.1.0.0'
        'RELEASE_10.1.1.0'          | '10.1.1.0'
    }

    def 'branch filter for special version for branches'() {
        when:
        ScmBranchFilter filter =  new ScmBranchFilter(new SimplePrefixConfig() as IPrefixConfig, BranchType.BRANCH, '', null, '', 2)

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName            | versionStr
        'SB_10.10'         | '10.10'
        'SB_10'            | '10'
        'SB_10-rc1'        | ''
        'SB_10.0-IS-4711'  | ''
    }

    def 'branch filter with different prefix separator configurations'() {
        when:
        SimplePrefixConfig prefixes = new SimplePrefixConfig()
        prefixes.stabilizationPrefix = 'enf7ga'
        prefixes.featurePrefix = 'enf7gapre'
        prefixes.tagPrefix = 'RELEASE'
        prefixes.tagPrefixSeperator = '_'
        prefixes.branchPrefixSeperator = '-'
        ScmBranchFilter filter = new ScmBranchFilter(prefixes, BranchType.BRANCH, '', null, '', 2)

        then:
        filter.getVersionStr('enf7ga-7.5') == '7.5'

        when:
        filter = new ScmBranchFilter( prefixes, BranchType.TAG, '', BranchType.BRANCH, '', 2)

        then:
        filter.getVersionStr('RELEASE_7.5.5.1') == '7.5.5.1'

    }

    def 'branch filter with different prefixes configurations - extended'() {
        when:
        SimplePrefixConfig pc = new SimplePrefixConfig()
        pc.tagPrefixSeperator = '_'
        pc.branchPrefixSeperator = '_'
        ScmBranchFilter filter = new ScmBranchFilter( pc, BranchType.HOTFIXBBRANCH , '', null, '', 2)

        then:
        filter.getVersionStr('HB_1.0') == '1.0'

        when:
        filter = new ScmBranchFilter(pc, BranchType.TAG, '', null, '', 2)

        then:
        filter.getVersionStr('RELEASE_7.5.5.1') == '7.5.5.1'

    }
}
