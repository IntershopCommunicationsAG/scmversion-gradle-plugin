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


package com.intershop.gradle.scm.utils

import groovy.transform.CompileStatic

/**
 * Contains the configuration of a key for SCM authentication.
 * This is currently only available for Git.
 */
@CompileStatic
class ScmKey {

    /**
     * Contructor, speficfies a default object with an empty
     * file and an empt passphrase.
     */
    ScmKey() {
        file = null
        passphrase = ''
    }

    /**
     * Contructor, speficfies a default object with a file and
     * passphrase.
     *
     * @param file key file
     * @param passphrase passphrase for the key file, if available and necessary
     */
    ScmKey(File file, String passphrase) {
        this.file = file
        this.passphrase = passphrase
    }

    /**
     * Key file
     */
    File file

    /**
     * Passphrase of the key
     */
    String passphrase

}
