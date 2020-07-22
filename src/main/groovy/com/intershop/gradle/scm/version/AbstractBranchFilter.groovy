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

abstract class AbstractBranchFilter {

    /**
     * Analyses the input parameter with the created filter and returns a valid
     * version string or an empty string, if the input is not valid or does not match.
     *
     * @param test input string
     * @return a valid version string or an empty string
     */
    abstract String getVersionStr(String test)

    /**
     * Analyses the input parameter with the created filter and returns the branch 
     * name as string or an empty string, if the input is not valid or does not match.
     *
     * @param test input string
     * @return the branch name as string or an empty string
     */
    abstract String getBranchNameStr(String test)

}
