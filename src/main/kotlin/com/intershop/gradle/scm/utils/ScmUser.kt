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
import javax.inject.Inject

/**
 * SCM user object
 */
open class ScmUser @Inject constructor(objectFactory: ObjectFactory) {

    private val nameProperty: Property<String> = objectFactory.property(String::class.java)
    private val passwordProperty: Property<String> = objectFactory.property(String::class.java)

    companion object {
        /**
         * Environment variable for name of scm user.
         */
        const val USERNAME = "SCM_USERNAME"

        /**
         * Environment variable for password scm user.
         */
        const val PASSWORD = "SCM_PASSWORD"
    }

    init {
        nameProperty.set((System.getProperty(USERNAME) ?: System.getenv(USERNAME) ?: "").toString().trim())
        passwordProperty.set((System.getProperty(PASSWORD) ?: System.getenv(PASSWORD) ?: "").toString().trim())
    }

    /**
     * This is the name of the SCM user.
     *
     * @property name
     */
    var name: String by nameProperty

    /**
     * This is the password of the SCM user.
     *
     * @property password
     */
    var password: String by passwordProperty
}