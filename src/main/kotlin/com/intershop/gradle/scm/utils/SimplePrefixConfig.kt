package com.intershop.gradle.scm.utils

class SimplePrefixConfig : IPrefixConfig {

    /**
     * Prefix for stabilization branches
     *
     * @property stabilizationPrefix
     */
    override var stabilizationPrefix: String = "SB"

    /**
     * Prefix for feature branches
     *
     * @property featurePrefix
     */
    override var featurePrefix: String = "FB"

    /**
     * Prefix for hotfix branches
     *
     * @property hotfixPrefix
     */
    override var hotfixPrefix: String = "HB"

    /**
     * Prefix for bugfix branches
     *
     * @property bugfixPrefix
     */
    override var bugfixPrefix: String = "BB"

    /**
     * Prefix for release tags
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
