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

import com.intershop.release.version.Version

/**
 * SCM version object.
 *
 * @constructor create a scm version object for version calculation
 * @property scmPath
 * @property version
 * @property changed
 */
open class ScmVersionObject constructor(val scmPath: String, var version: Version, var changed: Boolean = false){

    /**
     * Is this the default version? If the property
     * is true the version is the default version.
     *
     * @property defaultVersion
     */
    var defaultVersion = false

    /**
     * Is the version calculated from branch name
     * and not from a tag? If true, the version
     * is calculated from a branch obbject.
     *
     * @property fromBranchName
     */
    var fromBranchName = false

    /**
     * Update the version object of
     * this object.
     *
     * @param version version object
     */
    fun updateVersion(version: Version) {
        this.version = version
    }
}
