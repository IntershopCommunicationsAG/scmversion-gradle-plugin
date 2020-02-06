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

import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.ScmKey
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.AbstractBranchFilter
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.TagOpt
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class GitRemoteService(val localService: GitLocalService,
                            var user: ScmUser? = null,
                            private var key: ScmKey? = null) {

    companion object {
        protected val log: Logger = LoggerFactory.getLogger(this::class.java.name)
    }

    init {
        with(localService) {
            if (remoteUrl.isNotEmpty()) {
                with(remoteUrl) {
                    if (startsWith("http") && !user?.name.isNullOrEmpty() && !user?.password.isNullOrEmpty()) {
                        log.debug("User name {} and password is used.", user?.name)
                        credentials = UsernamePasswordCredentialsProvider(user?.name, user?.password)
                    } else if ((remoteUrl.startsWith("git@") || remoteUrl.startsWith("ssh://git@"))
                            && key?.file != null && key!!.file!!.exists()) {
                        log.debug("ssh connector is used with key {}.", key!!.file!!.absolutePath)
                        sshConnector = SSHConnector(key)
                    }
                }
            }
        }
    }

    private var credentials: CredentialsProvider? = null
    private var sshConnector: SSHConnector? = null

    val remoteConfigAvailable: Boolean
        get() {
            return (credentials != null || sshConnector != null)
        }

    /**
     * Calculates the rev object from revision id string
     *
     * @param rev
     * @return rev object from the Git repository
     */
    fun getObjectId( rev: String): RevObject {
        val id = localService.repository.resolve(rev)
        val rw = RevWalk(localService.client.repository)
        return rw.parseAny(id)
    }

    /**
     * Map with rev ids and assigned tag names.
     */
    fun getTagMap(branchFilter: AbstractBranchFilter): Map<String, BranchObject>  {
        //specify return value
        val rv = mutableMapOf<String, BranchObject>()

        // fetch all tags from repo
        if(remoteConfigAvailable) {
            fetchTagsCmd()
        }
        //specify walk
        val walk = RevWalk(localService.client.repository)

        //check tags and calculate
        localService.repository.refDatabase.getRefsByPrefix(Constants.R_TAGS).forEach { ref: Ref ->
            val tagName = ref.name.substring(Constants.R_TAGS.length)
            val version = branchFilter.getVersionStr(tagName)
            if(! version.isNullOrBlank()) {
                val rc = walk.parseCommit(ref.objectId)
                rv[ObjectId.toString(rc)] = BranchObject(ObjectId.toString(rc), version, tagName)
            }
        }

        walk.dispose()
        return rv
    }

    /**
     * fetch all tag information from remote branch
     * remote connection is necessary
     */
    fun fetchTagsCmd() {
        try {
            // fetch tags
            val cmd = localService.client.fetch()
            cmd.remote = "origin"
            cmd.setTagOpt(TagOpt.FETCH_TAGS)
            cmd.refSpecs = listOf(Transport.REFSPEC_TAGS)
            addCredentialsToCmd(cmd)
            cmd.call()
        } catch( nrex: InvalidRemoteException) {
            log.warn("No remote repository is available! {}", nrex.message)
        } catch( tex: TransportException) {
            tex.printStackTrace()
            log.warn("It was not possible to fetch all tags. Please check your credential configuration.", tex)
        }
    }

    /**
     * add credentials to the git command.
     *
     * @param cmd git command
     */
    open fun addCredentialsToCmd( cmd: TransportCommand<*, *>) {
        // add credentials to command
        if (credentials != null) {
            cmd.setCredentialsProvider(credentials)
        } else if (sshConnector != null) {
            cmd.setTransportConfigCallback {
                (it as SshTransport).sshSessionFactory = sshConnector
            }
        }
    }

    /**
     * fetch all changes from remote
     * remote connection is necessary
     */
    open fun fetchAllCmd() {
        try {
            // fetch all
            val cmd = localService.client.fetch()
            cmd.remote = "origin"
            cmd.isCheckFetchedObjects = true
            addCredentialsToCmd(cmd)
            cmd.call()
        } catch( nrex: InvalidRemoteException) {
            ScmVersionService.log.warn("No remote repository is available! {}", nrex.message)
        } catch( tex: TransportException) {
            ScmVersionService.log.warn(
                    "It was not possible to fetch all. Please check your credential configuration.",
                    tex)
        }
    }

    fun getFirstObjectId(): ObjectId? {
        var commitId: ObjectId? = null
        try {
            val headId = localService.repository.resolve(localService.revID)
            val walk = RevWalk( localService.repository)
            walk.sort(RevSort.TOPO)
            walk.markStart(walk.parseCommit(headId))

            var commit: RevCommit? = walk.next()
            var preCommit: RevCommit?
            do {
                preCommit = commit
                commit = walk.next()
            } while (commit != null)
            commitId = walk.parseCommit(preCommit)
        } catch ( ex: Exception ) {
            log.error("it was not possible to get first commit")
        }
        return commitId
    }
}
