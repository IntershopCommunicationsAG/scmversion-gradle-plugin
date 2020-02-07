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

/**
 * Simple prefix configuration for
 * testing.
 */
class SimplePrefixConfig : IPrefixConfig {

    /**
     * Prefix for stabilization branches.
     *
     * @property stabilizationPrefix
     */
    override var stabilizationPrefix: String = "SB"

    /**
     * Prefix for feature branches.
     *
     * @property featurePrefix
     */
    override var featurePrefix: String = "FB"

    /**
     * Prefix for hotfix branches.
     *
     * @property hotfixPrefix
     */
    override var hotfixPrefix: String = "HB"

    /**
     * Prefix for bugfix branches.
     *
     * @property bugfixPrefix
     */
    override var bugfixPrefix: String = "BB"

    /**
     * Prefix for release tags.
     *
     * @property tagPrefix
     */
    override var tagPrefix: String = "RELEASE"

    /**
     * Separator between prefix and version.
     *
     * @property prefixSeperator
     */
    override var prefixSeperator: String = "_"

    /**
     * Separator between prefix and version for branches.
     *
     * @property branchPrefixSeperator
     */
    override var branchPrefixSeperator: String? = null
    /**
     * Separator between prefix and version for tags.
     *
     * @property tagPrefixSeperator
     */
    override var tagPrefixSeperator: String? = null

}
