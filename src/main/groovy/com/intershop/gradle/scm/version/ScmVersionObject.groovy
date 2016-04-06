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

import com.intershop.release.version.Version
import groovy.transform.CompileStatic

/**
 * SCM version object
 */
@CompileStatic
class ScmVersionObject {

    private final Version version
    private final String scmpath

    boolean changed

    boolean defaultVersion = false
    boolean fromBranchName = false

    ScmVersionObject(String scmpath, Version version, boolean changed = false ) {
        this.version = version
        this.scmpath = scmpath
        this.changed = changed
    }

    public String getScmPath() {
        return scmpath
    }

    public Version getVersion() {
        return version
    }
}
