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
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.footer
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.getHeader
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.getLineChangedFile
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.getLineMessage
import com.intershop.release.version.Version
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

open class GitChangeLogService(val versionExt: VersionExtension,
                               val remoteService: GitRemoteService) : ScmChangeLogService {

    companion object {
        @JvmStatic
        protected val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    @Throws(GradleException::class)
    override fun createLog(changelogFile: File, targetVersion: String?) {

        try {
            var prevVersion = if (targetVersion.isNullOrBlank()) { versionExt.previousVersion } else { targetVersion }

            val pvt = if(! prevVersion.isNullOrBlank()) { versionExt.getPreviousVersionTag(prevVersion) } else { null }
            val pv = versionExt.versionService.preVersion

            if(pvt != null) {
                changelogFile.appendText(getHeader(pv.toString(), pvt.version.toString()))
            } else {
                changelogFile.appendText(getHeader(pv.toString(), "first commit"))
            }

            val objID = if(pvt != null) {
                            remoteService.getObjectId(pvt.branchObject.id)
                        } else {
                            (versionExt.versionService as GitVersionService).getFirstObjectId()
                        }

            val refs = remoteService.localService.client.log().addRange(
                    objID,
                    remoteService.getObjectId(remoteService.localService.revID)).call()

            refs.forEach {  rc ->
                changelogFile.appendText(getLineMessage(rc.getFullMessage(), rc.getName().substring(0, 8)))
                addFilesInCommit(changelogFile, rc)
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
        diffFmt.setPathFilter(TreeFilter.ANY_DIFF)
        diffFmt.setDetectRenames(true)

        val rw = RevWalk(remoteService.localService.repository)
        rw.parseHeaders(commit.getParent(0))

        val a = commit.getParent(0).getTree()
        val b = commit.getTree()

        diffFmt.scan(a, b).forEach {  e: DiffEntry ->

            when (e.getChangeType()) {
                DiffEntry.ChangeType.ADD    -> changelogFile.appendText(getLineChangedFile(e.getNewPath(), "A"))
                DiffEntry.ChangeType.DELETE -> changelogFile.appendText(getLineChangedFile(e.getOldPath(), "D"))
                DiffEntry.ChangeType.MODIFY -> changelogFile.appendText(getLineChangedFile(e.getNewPath(), "M"))
                DiffEntry.ChangeType.COPY   -> changelogFile.appendText(getLineChangedFile("${e.getOldPath()} ->\n${e.getNewPath()} (${e.getScore()})", "C"))
                DiffEntry.ChangeType.RENAME -> changelogFile.appendText(getLineChangedFile("${e.getOldPath()} ->\n${e.getNewPath()} (${e.getScore()})", "R"))
                else -> changelogFile.appendText("unknown change")
            }
        }
    }

}