package com.intershop.gradle.scm.services.file

import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.utils.PrefixConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

open class FileLocalService(projectDir: File,
                            prefixes: PrefixConfig) : ScmLocalService(projectDir, prefixes) {

    companion object {
        @JvmStatic
        protected val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    init {
        log.warn("This project is not included in a SCM!")
    }

    /**
     * It returns the remote url, calculated from the properties of the working copy (read only).
     * For this provider it is only the file path.
     *
     * @return remote url
     */
    override val remoteUrl: String
        get() = projectDir.toURI().toURL().toString()

    /**
     * The revision id from the working copy (read only).
     * For this provider it is always 'unknown'.
     *
     * @return revision id
     */
    override val revID: String
        get() = "unknown"

    /**
     * The base (stabilization) branch name of the current working copy
     */
    override val branchName: String
        get() = "trunk"

    /**
     * This is true, if the local working copy changed.
     *
     * @property changed
     */
    override val changed: Boolean
        get() = true

}
