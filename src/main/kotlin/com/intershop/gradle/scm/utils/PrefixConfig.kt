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
 * This is the configuration class for the necessary prefixes
 * on special branches, so that it is possible to identify the
 * relevant branches and tags for version calculation.
 */
abstract class PrefixConfig: IPrefixConfig {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val stabilizationPrefixProperty: Property<String> = objectFactory.property(String::class.java)
    private val featurePrefixxProperty: Property<String> = objectFactory.property(String::class.java)
    private val hotfixPrefixProperty: Property<String> = objectFactory.property(String::class.java)
    private val bugfixPrefixProperty: Property<String> = objectFactory.property(String::class.java)
    private val tagPrefixProperty: Property<String> = objectFactory.property(String::class.java)

    private val prefixSeperatorProperty: Property<String> = objectFactory.property(String::class.java)
    private val branchPrefixSeperatorProperty: Property<String> = objectFactory.property(String::class.java)
    private val tagPrefixSeperatorProperty: Property<String> = objectFactory.property(String::class.java)

    init {
        stabilizationPrefixProperty.convention("SB")
        featurePrefixxProperty.convention("FB")
        hotfixPrefixProperty.convention("HB")
        bugfixPrefixProperty.convention("BB")
        tagPrefixProperty.convention("RELEASE")

        prefixSeperatorProperty.convention("_")
    }

    /**
     * Prefix for stabilization branches
     *
     * @property stabilizationPrefix
     */
    override var stabilizationPrefix: String by stabilizationPrefixProperty

    /**
     * Prefix for feature branches
     */
    override var featurePrefix: String by featurePrefixxProperty

    /**
     * Prefix for hotfix branches
     *
     * @property stabilizationPrefix
     */
    override var hotfixPrefix: String by hotfixPrefixProperty

    /**
     * Prefix for bugfix branches
     *
     * @property bugfixPrefixProperty
     */
    override var bugfixPrefix: String by bugfixPrefixProperty

    /**
     * Prefix for release tags
     *
     * @property tagPrefixProperty
     */
    override var tagPrefix: String by tagPrefixProperty

    /**
     * Separator between prefix and version.
     *
     * @property prefixSeperatorProperty
     */
    override var prefixSeperator: String by prefixSeperatorProperty

    /**
     * Separator between prefix and version for branches.
     *
     * @property branchPrefixSeperator
     */
    override var branchPrefixSeperator: String?
        get() = branchPrefixSeperatorProperty.orNull
        set(value) = branchPrefixSeperatorProperty.set(value)

    /**
     * Separator between prefix and version for tags.
     *
     * @property tagPrefixSeperator
     */
    override var tagPrefixSeperator: String?
        get() = tagPrefixSeperatorProperty.orNull
        set(value) = tagPrefixSeperatorProperty.set(value)
}