/*
 * Copyright 2020 Intershop Communications AG.
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
package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.services.git.GitLocalService
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper
import com.intershop.gradle.scm.utils.ScmType
import com.intershop.gradle.scm.utils.getValue
import com.intershop.gradle.scm.utils.setValue
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.treewalk.filter.TreeFilter
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.provideDelegate
import java.io.BufferedOutputStream
import java.io.File
import javax.inject.Inject
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.getHeader
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.getMessageLine
import com.intershop.gradle.scm.utils.ChangeLogServiceHelper.footer

/**
 * This is the implementation of Gradle
 * task to create a change log from the
 * previous tag (target) and the current
 * tag.
 */
abstract class CreateChangeLog: AbstractDryRunTask() {

    /**
     * Inject service of ObjectFactory (See "Service injection" in Gradle documentation.
     */
    @get:Inject
    abstract val objectFactory: ObjectFactory

    private val changelogFileProperty: RegularFileProperty = objectFactory.fileProperty()
    private val prevVersionProperty: Property<String> = project.objects.property(String::class.java)

    private var startVersionView: String = ""

    init {
        description = "Creates a changelog based on SCM information in ASCIIDoc format"

        prevVersionProperty.convention("")
    }

    /**
     * This is the provider for the targetVersion.
     */
    fun providePrevVersion(prevVersion: Provider<String>) = prevVersionProperty.set(prevVersion)

    /**
     * This is the property with command line option to specify
     * the start version of the change log calculation.
     *
     * @property preVersion
     */
    @set:Option(option= "prevVersion", description="Specifies a special version for the changelog creation.")
    @get:Input
    var preVersion:String by prevVersionProperty

    /**
     * This is the start object of the
     * change log creation.
     *
     * @property startRevObject
     */
    @get:Optional
    @get:Input
    val startRevObject: RevObject? by lazy {
        var objectID: RevObject? = null
        val scmext = project.extensions.getByType(ScmExtension::class.java)

        if(scmext.scmType == ScmType.GIT) {
            val tempPrevVersion = if(prevVersionProperty.isPresent && prevVersionProperty.get().isNotEmpty()) {
                prevVersionProperty.orNull
            } else {
                scmext.version.previousVersion
            }
            with(scmext.localService as GitLocalService) {
                if (tempPrevVersion != null) {
                    val versiontag = scmext.version.getPreviousVersionTag(tempPrevVersion)
                    startVersionView = tempPrevVersion
                    objectID = getObjectId(versiontag.branchObject.id, repository)
                } else {
                    try {
                        val headId = repository.resolve(revID)
                        val walk = RevWalk(repository)
                        walk.sort(RevSort.TOPO)
                        walk.markStart(walk.parseCommit(headId))

                        var commit: RevCommit? = walk.next()
                        var preCommit: RevCommit?
                        do {
                            preCommit = commit
                            commit = walk.next()
                        } while (commit != null)
                        objectID = walk.parseCommit(preCommit)
                    } catch ( ex: Exception) {
                        throw GradleException("it was not possible to get first commit (${ex.message}) (${ex.cause}")
                    }
                    startVersionView = "first commit"
                }
            }
        }

        objectID
    }

    /**
     * This method calculates the end version of the
     * report. It returns an JGit object.
     *
     * @property endRevObject
     */
    @get:Optional
    @get:Input
    val endRevObject: RevObject? by lazy {
        var objectID: RevObject? = null
        val scmext = project.extensions.getByType(ScmExtension::class.java)
        if(scmext.scmType == ScmType.GIT) {
            with(scmext.localService as GitLocalService)
            {
                objectID = getObjectId(revID, repository)
            }
        }
        objectID
    }

    private fun getObjectId(id: String, repository: Repository): RevObject {
        val objid = repository.resolve(id)
        val rw = RevWalk(repository)
        return rw.parseAny(objid)
    }

    /**
     * Provider for output file property.
     */
    fun provideChangelogFile(changelogFile: Provider<RegularFile>) {
        changelogFileProperty.set(changelogFile)
    }

    /**
     * Input file property for output.
     *
     * @property changelogFile
     */
    @get:OutputFile
    var changelogFile: File
        get() = changelogFileProperty.get().asFile
        set(value) = changelogFileProperty.set(value)

    /**
     * Implementation of the task action.
     */
    @TaskAction
    fun createLog() {
        val scmConfig: ScmExtension = project.extensions.getByType(ScmExtension::class.java)

        if(scmConfig.scmType == ScmType.GIT) {
            if(! dryRun) {
                // set configuration parameter
                changelogFile.parentFile.mkdirs()
                if (changelogFile.exists()) {
                    changelogFile.delete()
                }

                with(changelogFile) {
                    changelogFile.createNewFile()

                    appendText(getHeader(startVersionView, scmConfig.version.version))
                    with(scmConfig.localService as GitLocalService) {
                        val refs = client.log().addRange(startRevObject, endRevObject).call()

                        refs.forEach { rc ->
                            appendText(getMessageLine(rc.fullMessage, rc.name.substring(0, GitLocalService.HASHLENGTH)))
                            addFilesInCommit(changelogFile, rc, this)
                        }
                    }
                    appendText(footer)
                }

                project.logger.info("Change log was written to {}", changelogFile.absolutePath)
            } else {
                println("""
                |--------------------------------------------------------------------------------------
                |        DryRun for ${this@CreateChangeLog.path}
                |        Changelog will created in ${changelogFile.absolutePath}
                |        
                |        Start rev: $startRevObject [${startVersionView}]
                |        End rev:   $endRevObject [${scmConfig.version.version}]
                |--------------------------------------------------------------------------------------""".trimMargin())
            }
        } else {
            project.logger.warn("The used scm does not support the creation of a change log.")
        }
    }

    private fun addFilesInCommit(changelogFile: File, commit: RevCommit, localService: GitLocalService) {
        val diffFmt = DiffFormatter( BufferedOutputStream(System.out) )
        diffFmt.setRepository(localService.repository)
        diffFmt.pathFilter = TreeFilter.ANY_DIFF
        diffFmt.isDetectRenames = true

        val rw = RevWalk(localService.repository)
        rw.parseHeaders(commit.getParent(0))

        val a = commit.getParent(0).tree
        val b = commit.tree

        diffFmt.scan(a, b).forEach {  e: DiffEntry ->
            changelogFile.appendText( processDiffEntry(e) )
        }
    }

    private fun processDiffEntry(e: DiffEntry): String {
        return when (e.changeType) {
            DiffEntry.ChangeType.ADD -> ChangeLogServiceHelper.getFileLine(e.newPath, "A")
            DiffEntry.ChangeType.DELETE -> ChangeLogServiceHelper.getFileLine(e.oldPath, "D")
            DiffEntry.ChangeType.MODIFY -> ChangeLogServiceHelper.getFileLine(e.newPath, "M")
            DiffEntry.ChangeType.COPY -> ChangeLogServiceHelper.
                    getFileLine("${e.oldPath} ->\n${e.newPath} (${e.score})", "C")
            DiffEntry.ChangeType.RENAME -> ChangeLogServiceHelper.
                    getFileLine("${e.oldPath} ->\n${e.newPath} (${e.score})", "R")
            else -> "unknown change"
        }
    }
}
