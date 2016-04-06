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

import com.intershop.gradle.scm.utils.PrefixConfig
import com.intershop.release.version.Version
import com.intershop.release.version.VersionType
import spock.lang.Specification

class ReleaseFilterSpec extends Specification {

    def 'test list of tags for simple versions'() {
        when:
        Version v = (new Version.Builder(VersionType.threeDigits)).build()
        ReleaseFilter filter =  new ReleaseFilter(new PrefixConfig(), v)

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                  | versionStr
        'RELEASE_1.1.1'          | '1.1.1'
        'RELEASE_1.2.3-rc1'      | '1.2.3-rc1'
        'RELEASE_1.2.3-rc.1'     | '1.2.3-rc.1'
        'RELEASE_1.2.3.4'        | ''
        'RELEASE_1.2.3-fb-1'     | ''
        'RELEASE_1.2.3-fb-1-rc.1'| ''
    }

    def 'test list of tags for simple versions for branch'() {
        when:
        Version v = (new Version.Builder(VersionType.threeDigits)).build()
        ReleaseFilter filter =  new ReleaseFilter(new PrefixConfig(), v.setBranchMetadata('fb'))

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                  | versionStr
        'RELEASE_1.1.1'          | ''
        'RELEASE_1.2.3-rc1'      | ''
        'RELEASE_1.2.3-rc.1'     | ''
        'RELEASE_1.2.3.4'        | ''
        'RELEASE_1.2.3-fb-rc1'   | '1.2.3-fb-rc1'
        'RELEASE_1.2.3-fb-rc.1'  | '1.2.3-fb-rc.1'
    }

    def 'test list of tags for simple versions for branch with JIRA issue'() {
        when:
        Version v = (new Version.Builder(VersionType.threeDigits)).build()
        ReleaseFilter filter =  new ReleaseFilter(new PrefixConfig(), v.setBranchMetadata('fb-1'))

        then:
        versionStr == filter.getVersionStr(tagName)

        where:
        tagName                  | versionStr
        'RELEASE_1.1.1'          | ''
        'RELEASE_1.2.3.4'        | ''
        'RELEASE_1.2.3-fb-1-rc1' | '1.2.3-fb-1-rc1'
        'RELEASE_1.2.3-fb-2-rc.1'| ''
    }
}
