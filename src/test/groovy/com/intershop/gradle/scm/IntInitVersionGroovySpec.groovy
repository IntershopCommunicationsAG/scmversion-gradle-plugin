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
package com.intershop.gradle.scm

import com.intershop.gradle.scm.test.utils.AbstractTaskGroovySpec

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class IntInitVersionGroovySpec extends AbstractTaskGroovySpec {

    final static String LOGLEVEL = "-i"

    def 'test with init GIT - #gradleVersion'(gradleVersion) {

        given:
        buildFile << """
        plugins {
            id 'com.intershop.gradle.scmversion'
        }

        scm {
            version{
                initialVersion = '11.0.0'
            }
        }

        version = scm.version.version

        """.stripIndent()
        'git init'.execute(null, testProjectDir)

        when:
        def result = getPreparedGradleRunner()
                .withArguments('showVersion', '--stacktrace', LOGLEVEL)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result.task(":showVersion").outcome == SUCCESS
        result.output.contains('Project version: 11.0.0')

        where:
        gradleVersion << supportedGradleVersions
    }
}
