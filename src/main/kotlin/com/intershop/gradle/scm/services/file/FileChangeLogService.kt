package com.intershop.gradle.scm.services.file

import com.intershop.gradle.scm.services.ScmChangeLogService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * File change log service implementation.
 * This is without functionality.
 */
class FileChangeLogService : ScmChangeLogService {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    /**
     * Implementation for the main function
     * without functionality.
     *
     * @param changelogFile
     * @param targetVersion
     */
    override fun createLog(changelogFile: File, targetVersion: String?) {
        log.warn("This function is unsupported scm for the change log creation.")
    }
}
