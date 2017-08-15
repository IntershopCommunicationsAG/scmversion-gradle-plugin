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
import spock.lang.Specification

class ScmBranchFilterSpec extends Specification {

    def 'trunk filter with prefix'() {
        when:
        PrefixConfig prefixes = new PrefixConfig('enf7ga', 'fbenf7ga', 'releases')
        prefixes.setPrefixSeperator('-')
        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.branch, prefixes, '', null, '', 2)

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
        PrefixConfig prefixes = new PrefixConfig('enf7ga', 'fbenf7ga', 'RELEASE')
        prefixes.setBranchPrefixSeperator('-')
        prefixes.setTagPrefixSeperator('_')

        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.tag, prefixes, 'enf7ga-7.4.6', BranchType.branch, '', 3)

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
        PrefixConfig prefixes = new PrefixConfig('SB', 'FB', 'RELEASES')
        prefixes.setPrefixSeperator('_')
        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.featureBranch, prefixes, '', null, 'ISTOOLS-12345', 2)

        then:
        versionStr == filter.getVersionStr(branchName)

        where:
        branchName                  | versionStr
        'FB_10.0-ISTOOLS-12345'     | '10.0-ISTOOLS-12345'
    }

    def 'branch filter with prefix'() {
        when:
        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.tag, new PrefixConfig(), '', null, '', 2)

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
        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.tag, new PrefixConfig(), '', null, 'fb1', 2)

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                  | versionStr
        'RELEASE_7.5.4.0-fb1-rc1'| '7.5.4.0-fb1-rc1'
    }

    def 'branch filter with prefix for feature branch'() {
        when:
        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.tag, new PrefixConfig(), '', null, 'fb1', 2)

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
        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.tag, new PrefixConfig(), 'SB_10', BranchType.branch, '', 2)

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
        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.tag, new PrefixConfig(), 'SB_10.0', BranchType.branch, '', 1)

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
        ScmBranchFilter filter =  new ScmBranchFilter(BranchType.branch, new PrefixConfig(), '', null, '', 2)

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
        PrefixConfig pc = new PrefixConfig('enf7ga', 'enf7gapre', 'RELEASE')
        pc.tagPrefixSeperator = '_'
        pc.branchPrefixSeperator = '-'
        ScmBranchFilter filter = new ScmBranchFilter(BranchType.branch, pc, '', null, '', 2)

        then:
        filter.getVersionStr('enf7ga-7.5') == '7.5'

        when:
        filter = new ScmBranchFilter(BranchType.tag, pc, '', null, '', 2)

        then:
        filter.getVersionStr('RELEASE_7.5.5.1') == '7.5.5.1'

    }

    def 'branch filter with different prefixes configurations - extended'() {
        when:
        PrefixConfig pc = new PrefixConfig('SB', 'FB', 'RELEASE', 'HB', 'BB')
        pc.tagPrefixSeperator = '_'
        pc.branchPrefixSeperator = '_'
        ScmBranchFilter filter = new ScmBranchFilter(BranchType.hotfixbBranch, pc, '', null, '', 2)

        then:
        filter.getVersionStr('HB_1.0') == '1.0'

        when:
        filter = new ScmBranchFilter(BranchType.tag, pc, '', null, '', 2)

        then:
        filter.getVersionStr('RELEASE_7.5.5.1') == '7.5.5.1'

    }
}
