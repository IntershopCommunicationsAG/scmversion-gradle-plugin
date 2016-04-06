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
import spock.lang.Specification

class PrefixConfigSpec extends Specification{

    def 'get FeatureBranchPattern'() {
        when:
        PrefixConfig config = new PrefixConfig()

        then:
        config.getFeatureBranchPattern() == "FB_(\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)-(.+)"
    }

    def 'test FeatureBranchPattern'() {
        when:
        PrefixConfig config = new PrefixConfig()
        def mfb = branchName =~ /${config.getFeatureBranchPattern()}/
        mfb.matches()

        then:
        mfb.hasGroup() == true
        mfb.size() == size
        mfb.matches() == match
        if(mfb.size() == 1) {
            mfb[0].size() == count
            mfb[0][count - 1] == fName
        }

        where:
        branchName             | match | size | count | fName
        'FB_1.0.0.0-IS-1234'   | true  | 1    | 6     | 'IS-1234'
        'FB_1.0.0-IS-12345'    | true  | 1    | 5     | 'IS-12345'
        'FB_1.0-IS-12345'      | true  | 1    | 3     | 'IS-12345'
        'FB_1.0.0.0.0-IS-12345'| false | 0    | 0     | ''
        'FB_1..0.0.0-IS-1234'  | false | 0    | 0     | ''
        'FB_1.0..0.0-IS-1234'  | false | 0    | 0     | ''
        'FB_1.0.0.0.-IS-1234'  | false | 0    | 0     | ''
        'FB_1.0.0.0-IS-1234-1' | true  | 1    | 6     | 'IS-1234-1'
        'FB_1.0.0.0'           | false | 0    | 0     | ''
        'f179a6a61e71baec877a8f06ee23620cde7e1ae6' | false | 0    | 0     | ''
    }
}
