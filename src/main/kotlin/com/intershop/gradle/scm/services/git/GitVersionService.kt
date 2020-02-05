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
import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.gradle.scm.version.AbstractBranchFilter
import com.intershop.gradle.scm.version.ReleaseFilter
import com.intershop.gradle.scm.version.ScmBranchFilter
import com.intershop.gradle.scm.version.ScmVersionObject
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.Version
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.LsRemoteCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.gradle.api.GradleException

open class GitVersionService(versionExt: VersionExtension, val remoteService: GitRemoteService): ScmVersionService(versionExt) {

    /**
     * The basic information service of this project.
     *
     * @property localService
     */
    override val localService: ScmLocalService
        get() = remoteService.localService

    /**
     * Returns an object from the SCM with additional information.
     *
     * @return version object from scm
     */
    override val versionObject: ScmVersionObject by lazy {
        var rv: ScmVersionObject? = null

        // identify headId of the working copy
        val headId = (localService as GitLocalService).repository.resolve(localService.revID)
        if(headId != null) {

            var pos = 0

            rv = getTagObject(getTagMap(getBranchFilter(BranchType.tag)),
                              getTagMap(ScmBranchFilter(localService.prefixes)),
                              headId)

            val branchFilter = getBranchFilter(
                    if (localService.featureBranchName.isNotEmpty()) { localService.branchType } else { BranchType.branch })

            // version from branch, if branch is available
            if (rv == null && localService.branchWithVersion && versionExt.versionBranchType == BranchType.branch) {

                rv = getBranchObject(getBranchMap(branchFilter), headId)
            }
            if(rv == null && localService.branchType == BranchType.trunk) {
                // version is calculated for the master branch from all release branches
                rv = getReleaseObject(getBranchMap(branchFilter))
            }
            if(rv == null && specialBranches.contains(localService.branchType)) {
                // version is calculated from the branch name
                var versionStr = branchFilter.getVersionStr(localService.branchName)
                if(! versionStr.isNullOrBlank()) {
                    rv = ScmVersionObject(localService.branchName, Version.forString(versionStr, versionExt.versionType), true)
                }
            }
            // tag is available, but there are commits between current rev and tag
            if (rv != null && (pos > 0 || localService.changed)) {
                rv.changed = true
            }
        }

        // fallback ...
        if(rv == null) {
            // check branch name
            rv = fallbackVersion
            if(! baseBranches.contains(localService.branchType)) {
                rv.updateVersion(rv.version.setBranchMetadata(localService.featureBranchName))
            }
            rv
        } else {
            rv
        }
    }

    /**
     * Returns a Map with version and associated version tag object
     *
     * @property versionTagMap map of version and version tag
     */
    override val versionTagMap: Map<Version, VersionTag> by lazy  {
        val branchMap = this.getTagMap(ReleaseFilter(localService.prefixes, preVersion))
        val versionTags = mutableMapOf<Version, VersionTag>()
        branchMap.forEach {_, bo ->
            var v = Version.valueOf(bo.version)
            versionTags.put(v, VersionTag(v, bo))
        }

        versionTags
    }

    /**
     * Moves the working copy to a specified version
     *
     * @param version
     * @param type Branchtype of the target branch
     * @return the revision id of the working after the move
     */
    @Throws(ScmException::class)
    override fun moveTo( version: String, type: BranchType): String {

        //checkout branch, wc is detached
        log.debug("git checkout {}", version)

        val branchName: String
        val path: String
        val versionMap = mutableMapOf<String, BranchObject>()

        if(checkBranch(BranchType.tag, version)) {
            branchName = getBranchName(BranchType.tag, version)
            versionMap.putAll(getTagMap(getBranchFilter(BranchType.tag)))
            path = "tags/"
        } else if(checkBranch(type, version)) {
            branchName = getBranchName(type, version)
            versionMap.putAll(getBranchMap( ScmBranchFilter(localService.prefixes, type,
                    localService.branchName, localService.branchType, ".*", versionExt.patternDigits)))
            path = "origin/"
        } else {
            throw ScmException("Version '${version}' does not exists")
        }

        var objectID = ""

        versionMap.forEach { key: String,  value: BranchObject ->
            if(value.name == branchName) {
                objectID = key
            }
        }

        if(objectID == "") {
            throw ScmException("Version '${version}' does not exists.")
        } else {
            log.info("Branch {} with id {} will be checked out.", branchName, objectID)
        }

        val cmd = (localService as GitLocalService).client.checkout()
        cmd.setName("${path}${branchName}")
        val ref: Ref? = cmd.call()

        log.debug("Reference is {}", ref)

        return objectID
    }

    fun getFirstObjectId(): ObjectId? {
        var commitId: ObjectId? = null
        try {
            val localService = remoteService.localService
            val localRepo = localService.repository
            val headId = localRepo.resolve(localService.revID)
            val walk = RevWalk(localRepo)
            walk.sort(RevSort.TOPO)
            walk.markStart(walk.parseCommit(headId))

            var commit: RevCommit? = walk.next()
            var preCommit: RevCommit? = null
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

    /**
     * Creates a tag with the specified version.
     *
     * @param version
     * @return the revision id of the tag
     */
    @Throws(ScmException::class)
    override fun createTag(version: String, rev: String?): String {
        // check if tag exits
        if (checkBranch(BranchType.tag, version)) {
            throw ScmException("Tag for ${version} exists on this repo.")
        }

         val tagName: String = getBranchName(BranchType.tag, version)

        // create tag
        val cmd = (localService as GitLocalService).client.tag()
            cmd.name = tagName
            cmd.setObjectId(remoteService.getObjectId(rev ?: localService.revID))
            cmd.message = "Tag ${tagName} created by gradle plugin"
            cmd.setAnnotated( true )
            cmd.setForceUpdate( false )

            val ref = cmd.call()

            // push changes to remote
            pushCmd()
            log.info("Tag ${tagName} was create on ${this.localService.branchName}")
            return ref.toString()
    }

    /**
     * Creates a branch with the specified version.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the branch
     */
    @Throws(ScmException::class)
     override fun createBranch(version: String,  featureBranch: Boolean, rev: String? ): String {
        val branchType = if(featureBranch) { localService.branchType } else { BranchType.branch }

        // check if branch exits
        if (checkBranch(branchType, version) ) {
            throw ScmException("Branch for ${version} exists in this repo.")
        }
        val branchName = getBranchName(branchType, version)

        // create branch
        val cmd = (localService as GitLocalService).client.branchCreate()
        cmd.setName(branchName)
        cmd.setStartPoint(rev ?: this.localService.revID)
        cmd.setForce(true)
        val ref = cmd.call()

        // push changes to remote
        pushCmd()
        log.info("Branch {} was created", branchName)
        return ref.toString()
    }

    /**
     * Returns true, if the specified release version is available.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    override fun isReleaseVersionAvailable( version: String): Boolean {
        return checkBranch(BranchType.tag, version)
    }

    /**
     * Returns a list of version and tags/branches
     * @param branchFilter
     * @return map
     */
    override fun getTagMap(branchFilter: AbstractBranchFilter): Map<String, BranchObject> {
        return remoteService.getTagMap(branchFilter)
    }

    /**
     * push changes (tag/branch) to remote
     * remote connection is necessary
     */
    @Throws(GradleException::class)
    private fun pushCmd() {
        // push changes
        try {
            val cmd = (localService as GitLocalService).client.push()
            remoteService.addCredentialsToCmd(cmd)
            cmd.setPushAll()
            cmd.setPushTags()
            cmd.remote =  "origin"
            cmd.setForce( true )
            cmd.call()
        } catch( gitEx: GitAPIException) {
            log.error(gitEx.message, gitEx.cause)
            throw GradleException(gitEx.message!!, gitEx.cause)
        } catch(tEx: TransportException) {
            log.error(tEx.message, tEx.cause)
            throw GradleException(tEx.message!!, tEx.cause)
        }
    }

    private fun getTagObject(tags: Map<String, BranchObject>,
                             simpleTags: Map<String, BranchObject>,
                             headId: ObjectId): ScmVersionObject? {
        // version from tag, if tag is available
        var rv: ScmVersionObject? = null

        if (!(tags.isEmpty() && simpleTags.isEmpty())) {
            val localService = remoteService.localService
            var pos = 0
            val walk = RevWalk(localService.repository)
            walk.sort(RevSort.TOPO)
            walk.markStart(walk.parseCommit(headId))

            var commit = walk.next()
            do {
                    val tagObject = tags[commit.id.name()] ?: if (!localService.branchWithVersion) {
                        simpleTags[commit.id.name]
                    } else {
                        null
                    }

                    if (tagObject != null) {
                        // commit is a tag
                        if (pos == 0) {
                            log.info("Version from tag {}", tagObject.name)
                            // commit is a tag with version information
                            rv = ScmVersionObject(tagObject.name, getVersionFrom(tagObject.version), false)
                            break
                        }
                        if (pos != 0 && versionExt.versionBranchType == BranchType.tag) {
                            log.info("Version from tag {}, but there are {} changes.", tagObject.name, pos)

                            rv = ScmVersionObject(localService.branchName, getVersionFrom(tagObject.version), true)

                            if (localService.branchType != BranchType.trunk &&
                                    localService.branchType != BranchType.branch &&
                                    localService.branchType != BranchType.tag) {
                                rv.updateVersion(rv.version.setBranchMetadata(localService.featureBranchName))
                            }
                            break
                        }
                        if (pos != 0 && versionExt.versionBranchType != BranchType.tag) {
                            break
                        }
                    } else {
                        ++pos
                        log.info("Next step in walk to tag from {}", commit.id.name)
                    }
                    commit = walk.next()
            } while (commit != null)

            if (localService.branchType != BranchType.tag && ! localService.branchWithVersion && rv != null) {
                rv.changed = (pos != 0) || localService.changed

                if (rv.changed && log.isInfoEnabled()) {
                    if (pos > 0) { log.info("There are {} commits after the last tag.", pos) }
                    if (localService.changed) { log.info("There are local changes in the repository.") }
                }

                rv.fromBranchName = localService.branchType != BranchType.trunk
                rv.updateVersion(rv.version.setBranchMetadata(localService.featureBranchName))
            }
        }
        return rv
    }

    private fun getBranchObject(branches: Map<String, BranchObject>,
                                headId: ObjectId): ScmVersionObject? {
        var rv: ScmVersionObject? = null

        if(! branches.isEmpty()) {
            val localService = remoteService.localService

            var pos = 0
            val walk = RevWalk(localService.repository)
            walk.sort(RevSort.TOPO)
            walk.markStart(walk.parseCommit(headId))

            var commit = walk.next()
            do {
                val branchObject = branches[commit.id.name()]
                if (branchObject != null) {
                    log.debug("Version from branch {}", branchObject)
                    rv = ScmVersionObject(branchObject.name, getVersionFrom(branchObject.version), true)
                    rv.fromBranchName = (branchObject.name == localService.branchName)
                    break
                } else {
                    ++pos
                    log.debug("Next step in walk to branch from {}", commit.id.name)
                }

                commit = walk.next()
            } while (commit != null)

            walk.dispose()
        }
        return rv
    }

    private fun getReleaseObject(branches: Map<String, BranchObject>) : ScmVersionObject? {
        var rv: ScmVersionObject? = null
        if(! branches.isEmpty()) {
            var branchVersions = mutableListOf<Version>()
            var branchVersion: String
            branches.forEach { _ , bo: BranchObject ->
                branchVersion = bo.version
                branchVersions.add(getVersionFrom(branchVersion))
            }
            var l: List<Version> = branchVersions.sorted()
            if (l.size > 0) {
                log.debug("Version found and latest will be used.")
                rv = ScmVersionObject(remoteService.localService.branchName, l.last(), true)
            }
        }

        return rv
    }

    private fun getVersionFrom(versionStr: String): Version {
        return Version.forString(versionStr, versionExt.versionType)
    }

    /**
     * Map with rev ids and assigned branche names.
     */
    private fun getBranchMap(branchFilter: AbstractBranchFilter): Map<String, BranchObject> {
        //specify return value
        val rv = mutableMapOf<String, BranchObject>()

        if(remoteService.remoteConfigAvailable) {
            fetchAllCmd()
        }

        //specify walk
        val walk = RevWalk(remoteService.localService.repository)

        val cmd = remoteService.localService.client.branchList()
        cmd.setListMode(ListBranchCommand.ListMode.ALL)
        val refList: List<Ref> = cmd.call()

        refList.forEach { ref: Ref ->
            val rc = walk.parseCommit(ref.objectId)
            val name = ref.getName().toString()
            val branchName = name.substring(name.lastIndexOf('/') + 1)

            if(branchName != "master") {
                val version = branchFilter.getVersionStr(branchName)
                if(version.isNotEmpty()) {
                    rv.put(ObjectId.toString(rc), BranchObject(ObjectId.toString(rc), version, branchName))
                }
            }
        }
        walk.dispose()

        return rv
    }

    /**
     * Check if branch of special version with special branch type exists.
     *
     * @param type
     * @param version
     * @return true if the branch exists
     */
    private fun checkBranch( type: BranchType,  version: String): Boolean {
        val name = getBranchName(type, version)
        val path = if(type == BranchType.tag) {
                        remoteService.fetchTagsCmd()
                        "refs/tags/"
                    } else {
                        fetchAllCmd()
                        "refs/heads/"
                    }

        // list all tags and branches
        val cmd: LsRemoteCommand = Git.lsRemoteRepository()
        remoteService.addCredentialsToCmd(cmd)
        cmd.setRemote(localService.remoteUrl)
        cmd.setHeads(true)
        cmd.setTags(true)

        // check if tag or branch is available
        val refs: Collection<Ref> = cmd.call()

        val rv = mutableListOf<String>()
        refs.forEach { r: Ref ->
            if("${path}${name}".equals(r.getName()) && r.getName() != null) {
                rv.add(r.getName())
            }
        }
        return rv.size > 0
    }

    /**
     * fetch all changes from remote
     * remote connection is necessary
     */
    private fun fetchAllCmd() {
        try {
            // fetch all
            val cmd = remoteService.localService.client.fetch()
            cmd.remote = "origin"
            cmd.setCheckFetchedObjects(true)
            remoteService.addCredentialsToCmd(cmd)
            cmd.call()
        } catch( nrex: InvalidRemoteException) {
            log.warn("No remote repository is available! {}", nrex.message)
        } catch( tex: TransportException) {
            log.warn("It was not possible to fetch all. Please check your credential configuration.", tex)
        }
    }
}