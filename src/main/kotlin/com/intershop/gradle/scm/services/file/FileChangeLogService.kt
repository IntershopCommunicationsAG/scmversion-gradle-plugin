package com.intershop.gradle.scm.services.file

import com.intershop.gradle.scm.services.ScmChangeLogService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class FileChangeLogService : ScmChangeLogService {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    override fun createLog(changelogFile: File, targetVersion: String?) {
        log.warn("This function is unsupported scm for the change log creation.")
    }
}