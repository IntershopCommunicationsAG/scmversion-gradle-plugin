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
package com.intershop.gradle.scm.utils

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.io.File
import javax.inject.Inject

/**
 * Contains the configuration of a key for SCM authentication.
 * This is currently only available for Git.
 */
open class ScmKey @Inject constructor(objectFactory: ObjectFactory) {

    private val fileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val passphraseProperty: Property<String> = objectFactory.property(String::class.java)

    companion object {
        /**
         * Key file for SCM access.
         */
        const val KEYFILE = "SCM_KEYFILE"

        /**
         * Passphrase of key file for SCM access.
         */
        const val PASSPHRASE = "SCM_KEYPASSPHRASE"
    }

    init {
        val filePath = (System.getProperty(KEYFILE) ?: System.getenv(KEYFILE) ?: "").toString().trim()
        var fileTemp: File? = null
        if (filePath.isNotEmpty()) {
            fileTemp = File(filePath)
        }
        if (fileTemp == null || !fileTemp.exists() || !fileTemp.canRead()) {
            fileTemp = File(System.getProperty("user.home"), ".ssh/id_dsa")
        }
        fileProperty.set(fileTemp)

        passphraseProperty.set( (System.getProperty(PASSPHRASE) ?: System.getenv(PASSPHRASE) ?: "").toString().trim() )
    }

    /**
     * Key file property.
     *
     * @property file
     */
    var file: File?
        get() = fileProperty.orNull?.asFile
        set(value) = fileProperty.set(value)

    /**
     * Passphrase property of the key.
     *
     * @property passphrase
     */
    var passphrase: String by passphraseProperty

}