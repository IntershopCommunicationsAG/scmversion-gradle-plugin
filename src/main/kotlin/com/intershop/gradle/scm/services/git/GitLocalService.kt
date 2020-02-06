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

import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.PrefixConfig
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.revwalk.RevWalk
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * This is the container of all information from an existing
 * working copy without any access to the remote location of the project.
 */
class GitLocalService(projectDir: File,
                      prefixes: PrefixConfig) : ScmLocalService(projectDir, prefixes) {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        /**
         * Length of Git hash refids.
         */
        const val HASHLENGTH = 8
    }

    /**
     * Returns the GIT repository
     * implementation for the local repository.
     */
    val repository: Repository by lazy {
        RepositoryBuilder().readEnvironment().findGitDir(projectDir).build()
    }

    /**
     * Returns the GIT client implementation
     * for the local repository.
     */
    val client: Git by lazy {
        Git(repository)
    }

    /**
     * The base (stabilization) branch name of the current working copy.
     *
     * @property branchName
     */
    override val branchName: String by lazy {
        val tagname = tagNameOnHead
        if(tagname.isNotEmpty()) { tagname } else { repository.branch }
    }

    /**
     * It returns the remote url, calculated from the properties of the working copy (read only).
     *
     * @property remoteUrl remote url
     */
    override val remoteUrl: String
        get() {
            return repository.config.getString("remote", "origin", "url")
        }
    
    /**
     * The revision id from the working copy (read only).
     *
     * @property revID revision id
     */
    override val revID: String
        get() {
            val id = repository.resolve(Constants.HEAD)
            return if(id != null) { id.name } else { "" }
        }

    /**
     * This is true, if the local working copy changed.
     *
     * @property changed
     */
    override val changed: Boolean by lazy {
        val status = client.status().call()
        val rv = status.untracked.size > 0 || status.uncommittedChanges.size > 0 ||
                status.removed.size > 0 || status.added.size > 0 ||
                status.changed.size > 0 || status.modified.size > 0

        if(log.isInfoEnabled && rv) {
            log.info("There are local changes on the repository.")
            if(status.untracked.size > 0) {
                status.untracked.forEach {
                    log.info("GIT: This file is not indexed {}", it)
                }
                status.removed.forEach {
                    log.info("GIT: This file is deleted {}", it)
                }
                status.added.forEach {
                    log.info("GIT: This file is added {}", it)
                }
                status.changed.forEach {
                    log.info("GIT: This file is changed {}", it)
                }
                status.modified.forEach {
                    log.info("GIT: This file is modified {}", it)
                }
                status.uncommittedChanges.forEach {
                    log.info("GIT: This file is uncommitted {}", it)
                }
            }
        }
        rv
    }

    private val tagNameOnHead: String by lazy {
        var rvTagName = ""
        val rw = RevWalk(repository)

        repository.refDatabase.getRefsByPrefix(Constants.R_TAGS).forEach { ref: Ref ->
            if(ObjectId.toString(rw.parseCommit(ref.objectId).id) == revID) {
                rvTagName = ref.name.substring(Constants.R_TAGS.length)
            }
        }
        rw.dispose()
        rvTagName
    }

    init {
        branchType = BranchType.MASTER

        if(branchName != "master") {
            val mfb = Regex(prefixes.featureBranchPattern).matchEntire(branchName)
            val mhb =  Regex(prefixes.hotfixBranchPattern).matchEntire(branchName)
            val mbb =  Regex(prefixes.bugfixBranchPattern).matchEntire(branchName)
            val msb =  Regex(prefixes.stabilizationBranchPattern).matchEntire(branchName)

            if(branchName == revID) {
                branchType = BranchType.DETACHEDHEAD
                log.info("Repo is in detached mode! Create a tag on {}.", branchName)
            } else if(mfb != null && mfb.groups.size > 1) {
                branchType = BranchType.FEATUREBRANCH
                featureBranchName = mfb.groupValues.last()
            } else if(mhb != null && mhb.groups.size > 1) {
                branchType = BranchType.HOTFIXBBRANCH
                featureBranchName = mhb.groupValues.last()
            } else if(mbb != null && mbb.groups.size > 1) {
                branchType = BranchType.BUGFIXBRANCH
                featureBranchName = mbb.groupValues.last()
            } else if(msb != null && msb.groups.size > 1) {
                branchType = BranchType.BRANCH
            } else {
                branchType = BranchType.FEATUREBRANCH
                featureBranchName = branchName
            }

            if(tagNameOnHead == branchName) {
                branchType = BranchType.TAG
            }
        }

        log.info("Branch name is {} and branch type {}", branchName, branchType)
    }
}
