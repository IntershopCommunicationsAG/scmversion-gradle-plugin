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
package com.intershop.gradle.scm.services.git

import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.services.ScmChangeLogService
import com.intershop.gradle.scm.services.git.GitLocalService.Companion.HASHLENGTH
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.footer
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.getHeader
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.getFileLine
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.getMessageLine
import org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD
import org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE
import org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY
import org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY
import org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.File

/**
 * Git change log service implementation calculates
 * changes between two versions.
 *
 * @param versionExt extension of the plugin
 * @param remoteService Git remote service
 */
open class GitChangeLogService(private val versionExt: VersionExtension,
                               private val remoteService: GitRemoteService) : ScmChangeLogService {

    companion object {
        @JvmStatic
        protected val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    /**
     * This is the main method of this servcie.
     * @param changelogFile target file for the changes
     * @param targetVersion start version
     */
    @Throws(GradleException::class)
    override fun createLog(changelogFile: File, targetVersion: String?) {

        try {
            val prevVersion = if (targetVersion.isNullOrBlank()) { versionExt.previousVersion } else { targetVersion }

            val pvt = if(! prevVersion.isNullOrBlank()) { versionExt.getPreviousVersionTag(prevVersion) } else { null }
            val pv = versionExt.versionService.preVersion

            if(pvt != null) {
                changelogFile.appendText(getHeader(pv.toString(), pvt.version.toString()))
            } else {
                changelogFile.appendText(getHeader(pv.toString(), "first commit"))
            }

            with(remoteService) {
                val objID = if (pvt != null) { getObjectId(pvt.branchObject.id) } else { firstObjectId }

                val refs = localService.client.log().addRange( objID, getObjectId(localService.revID)).call()

                refs.forEach { rc ->
                    changelogFile.appendText(getMessageLine(rc.fullMessage, rc.name.substring(0, HASHLENGTH)))
                    addFilesInCommit(changelogFile, rc)
                }
            }
            changelogFile.appendText(footer)
        } catch( ex: Exception) {
            log.debug(ex.message)
            throw GradleException("It is not possible to create a log file. ${ex.message}", ex.cause)
        }

    }

    private fun addFilesInCommit(changelogFile: File, commit: RevCommit) {
        val diffFmt = DiffFormatter( BufferedOutputStream(System.out) )
        diffFmt.setRepository(remoteService.localService.repository)
        diffFmt.pathFilter = TreeFilter.ANY_DIFF
        diffFmt.isDetectRenames = true

        val rw = RevWalk(remoteService.localService.repository)
        rw.parseHeaders(commit.getParent(0))

        val a = commit.getParent(0).tree
        val b = commit.tree

        diffFmt.scan(a, b).forEach {  e: DiffEntry ->
            changelogFile.appendText( processDiffEntry(e) )
        }
    }

    private fun processDiffEntry(e: DiffEntry): String {
        return when (e.changeType) {
            ADD -> getFileLine(e.newPath, "A")
            DELETE -> getFileLine(e.oldPath, "D")
            MODIFY -> getFileLine(e.newPath, "M")
            COPY -> getFileLine("${e.oldPath} ->\n${e.newPath} (${e.score})", "C")
            RENAME -> getFileLine("${e.oldPath} ->\n${e.newPath} (${e.score})", "R")
            else -> "unknown change"
        }
    }
}
