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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * SCM user object.
 */
open class ScmUser @Inject constructor(objectFactory: ObjectFactory) {

    private val nameProperty: Property<String> = objectFactory.property(String::class.java)
    private val passwordProperty: Property<String> = objectFactory.property(String::class.java)

    init {
        passwordProperty.isPresent
    }

    /**
     * Provider for name property.
     */
    val nameProvider: Provider<String> = nameProperty

    val nameIsAvailable: Boolean
        get() = nameProvider.isPresent && nameProvider.getOrElse("").isNotEmpty()

    /**
     * This is the name of the SCM user.
     *
     * @property name
     */
    var name: String by nameProperty

    /**
     * Provider for password property.
     */
    val passwordProvider: Provider<String> = passwordProperty

    val passwordIsAvailable: Boolean
        get() = passwordProvider.isPresent && passwordProvider.getOrElse("").isNotEmpty()

    /**
     * This is the password of the SCM user.
     *
     * @property password
     */
    var password: String by passwordProperty
}
