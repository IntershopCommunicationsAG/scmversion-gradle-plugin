/*
 * Copyright 2020 Intershop Communications AG.
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

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

/**
 * Contains the configuration of a key for SCM authentication.
 * This is currently only available for Git.
 */
open class ScmKey @Inject constructor(objectFactory: ObjectFactory) {

    private val fileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val passphraseProperty: Property<String> = objectFactory.property(String::class.java)

    /**
     * Provider for name property.
     *
     * @property fileProvider for SSH key file.
     */
    val fileProvider: Provider<RegularFile> = fileProperty

    /**
     * file check for configuration of the SSH key file.
     *
     * @returns true if file is available.
     */
    val fileIsAvailable: Boolean
        get() = fileProvider.isPresent && fileProvider.orNull != null && fileProvider.get().asFile.exists()

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
