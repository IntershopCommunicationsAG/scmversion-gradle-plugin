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
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is the implementation of a version
 * service based on GIT.
 *
 * @constructor creates a service based on GIT
 * @param versionExt main extension of this plugin
 * @param remoteService remote service for GIt functionality
 */
open class GitVersionService
        (versionExt: VersionExtension, private val remoteService: GitRemoteService):
        ScmVersionService(versionExt) {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)

        /**
         * Max length of the branch name
         */
        const val MAX_BRANCHNAME_LENGTH = 250
    }

    /**
     * The basic information service of this project.
     *
     * @property localService
     */
    override val localService: ScmLocalService
        get() = remoteService.localService

    private val gitService: GitLocalService
        get() = remoteService.localService

    /**
     * Returns an object from the SCM with additional information.
     *
     * @return version object from scm
     */
    override val versionObject: ScmVersionObject by lazy {
        var rv: ScmVersionObject? = null

        // identify headId of the working copy
        val headId = gitService.repository.resolve(localService.revID)
        if(headId != null) {

            rv = getTagObject(getTagMap(getBranchFilter(BranchType.TAG)),
                              getTagMap(ScmBranchFilter(localService.prefixes)),
                              headId)

            val branchFilter = getBranchFilter(localService.getBranchType(localService.featureBranchName.isNotEmpty()))
            val branchMap = getBranchMap(branchFilter)

            // version from branch, if branch is available
            rv = rv ?: if (localService.branchWithVersion && versionExt.versionBranchType == BranchType.BRANCH) {
                getBranchObject(branchMap, headId) } else { null }

            // version is calculated for the master branch from all release branches
            rv = rv ?: if(localService.branchType == BranchType.MASTER) {
                getReleaseObject(branchMap) } else { null }

            if(rv == null && specialBranches.contains(localService.branchType)) {
                // version is calculated from the branch name
                val versionStr = branchFilter.getVersionStr(localService.branchName)
                rv = if(! versionStr.isBlank()) {
                        ScmVersionObject(localService.branchName,
                                         Version.forString(versionStr, versionExt.versionType),
                                         true)
                    } else { null }
            }
        }

        rv ?: fallbackVersion
    }

    /**
     * Returns a Map with version and associated version tag object.
     *
     * @property versionTagMap map of version and version tag
     */
    override val versionTagMap: Map<Version, VersionTag> by lazy  {
        val branchMap = this.getTagMap(ReleaseFilter(localService.prefixes, preVersion))
        val versionTags = mutableMapOf<Version, VersionTag>()
        branchMap.forEach { (_, bo) ->
            val v = Version.valueOf(bo.version)
            versionTags[v] = VersionTag(v, bo)
        }

        versionTags
    }

    /**
     * Moves the working copy to a specified version.
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

        when {
            checkBranch(BranchType.TAG, version) -> {
                branchName = getBranchName(BranchType.TAG, version)
                path = "tags/"
                versionMap.putAll(getTagMap(getBranchFilter(BranchType.TAG)))
            }
            checkBranch(type, version) -> {
                branchName = getBranchName(type, version)
                versionMap.putAll(getBranchMap( ScmBranchFilter(localService.prefixes, type,
                        localService.branchName, localService.branchType, ".*", versionExt.patternDigits)))
                path = "origin/"
            }
            else -> throw ScmException("Version '${version}' does not exists")
        }

        var objectID = ""

        versionMap.forEach { (key: String, value: BranchObject) ->
            if(value.name == branchName) {
                objectID = key
            }
        }

        if(objectID == "") {
            throw ScmException("Version '${version}' does not exists.")
        } else {
            log.info("Branch {} with id {} will be checked out.", branchName, objectID)
        }

        val cmd = gitService.client.checkout()
        cmd.setName("${path}${branchName}")
        val ref: Ref? = cmd.call()

        log.debug("Reference is {}", ref)

        return objectID
    }

    /**
     * Creates a tag with the specified version.
     *
     * @param version
     * @return the revision id of the tag
     */
    @Throws(ScmException::class)
    override fun createTag(version: String): String {
        // check if tag exits
        if (checkBranch(BranchType.TAG, version)) {
            throw ScmException("Tag for $version exists on this repo.")
        }

         val tagName: String = getBranchName(BranchType.TAG, version)

        // create tag
        val cmd = gitService.client.tag()
        cmd.name = tagName
        cmd.objectId = remoteService.getObjectId(localService.revID)
        cmd.message = "Tag $tagName created by gradle plugin"
        cmd.isAnnotated = true
        cmd.isForceUpdate = false

        val ref = cmd.call()

        // push changes to remote
        pushCmd()
        log.info("Tag $tagName was create on ${localService.branchName}")
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
     override fun createBranch(version: String,  featureBranch: Boolean): String {
        val branchType = localService.getBranchType(featureBranch)

        // check if branch exits
        if (checkBranch(branchType, version) ) {
            throw ScmException("Branch for $version exists in this repo.")
        }
        var branchName = getBranchName(branchType, version)

        if(branchName.length > MAX_BRANCHNAME_LENGTH) {
            log.warn("Branchname {} will be reduced to a length of {}", branchName, MAX_BRANCHNAME_LENGTH)
            branchName = branchName.substring(0, MAX_BRANCHNAME_LENGTH)
        }

        // create branch
        val cmd = gitService.client.branchCreate().setName(branchName).
                    setStartPoint(localService.revID).setForce(true)
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
        return checkBranch(BranchType.TAG, version)
    }

    /**
     * Returns a list of version and tags/branches.
     * @param branchFilter
     * @return map
     */
    override fun getTagMap(branchFilter: AbstractBranchFilter): Map<String, BranchObject> {
        return remoteService.getTagMap(branchFilter)
    }

    /**
     * Push changes (tag/branch) to remote.
     */
    @Throws(GradleException::class)
    private fun pushCmd() {
        // push changes
        try {
            val cmd = gitService.client.push()
            remoteService.addCredentialsToCmd(cmd)
            cmd.setPushAll().setPushTags()
            cmd.remote =  "origin"
            cmd.isForce = true
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
                val tagObject = tags[commit.id.name()] ?:
                    if (!localService.branchWithVersion) { simpleTags[commit.id.name] } else { null }

                if (tagObject != null) {
                    // commit is a tag
                    if (pos == 0) {
                        log.info("Version from tag {}", tagObject.name)
                        // commit is a tag with version information
                        rv = ScmVersionObject(tagObject.name, getVersionFrom(tagObject.version), false)
                        updateVersionObject(rv, pos, localService.branchType != BranchType.MASTER)
                        break
                    }
                    if (pos != 0 && versionExt.versionBranchType == BranchType.TAG) {
                        log.info("Version from tag {}, but there are {} changes.", tagObject.name, pos)

                        rv = ScmVersionObject(localService.branchName, getVersionFrom(tagObject.version), true)
                        updateVersionObject(rv, pos, localService.branchType != BranchType.MASTER)
                        if (! baseBranches.contains(localService.branchType)) {
                            rv.updateVersion(rv.version.setBranchMetadata(localService.featureBranchName))
                        }
                        break
                    }
                    if (pos != 0 && versionExt.versionBranchType != BranchType.TAG) {
                        break
                    }
                } else {
                    ++pos
                    log.info("Next step in walk to tag from {}", commit.id.name)
                }
                commit = walk.next()
            } while (commit != null)
        }
        return rv
    }

    private fun getBranchObject(branches: Map<String, BranchObject>,
                                headId: ObjectId): ScmVersionObject? {
        var rv: ScmVersionObject? = null

        if(branches.isNotEmpty()) {
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
                    updateVersionObject(rv, pos, branchObject.name == localService.branchName)
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

    private fun updateVersionObject(vobj: ScmVersionObject, commits: Int, isFromBranchName: Boolean) {
        vobj.changed = commits > 0 || localService.changed
        if (vobj.changed && log.isInfoEnabled) {
            if (commits > 0) { log.info("There are {} commits after the last tag.", commits) }
            if (localService.changed) { log.info("There are local changes in the repository.") }
        }
        vobj.fromBranchName = isFromBranchName
    }

    private fun getReleaseObject(branches: Map<String, BranchObject>) : ScmVersionObject? {
        var rv: ScmVersionObject? = null
        if(branches.isNotEmpty()) {
            val branchVersions = mutableListOf<Version>()
            var branchVersion: String
            branches.forEach { (_, bo: BranchObject) ->
                branchVersion = bo.version
                branchVersions.add(getVersionFrom(branchVersion))
            }
            val l: List<Version> = branchVersions.sorted()
            if (l.isNotEmpty()) {
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
            remoteService.fetchAllCmd()
        }

        //specify walk
        val walk = RevWalk(remoteService.localService.repository)

        val cmd = remoteService.localService.client.branchList()
        cmd.setListMode(ListBranchCommand.ListMode.ALL)
        val refList: List<Ref> = cmd.call()

        refList.forEach { ref: Ref ->
            val rc = walk.parseCommit(ref.objectId)
            val name = ref.name.toString()
            val branchName = name.substring(name.lastIndexOf('/') + 1)

            if(branchName != "master") {
                val version = branchFilter.getVersionStr(branchName)
                if(! version.isNullOrEmpty()) {
                    rv[ObjectId.toString(rc)] = BranchObject(ObjectId.toString(rc), version, branchName)
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
        val path: String

        if(type == BranchType.TAG) {
            remoteService.fetchTagsCmd()
            path = "refs/tags/${getBranchName(type, version)}"
        } else {
            remoteService.fetchAllCmd()
            path = "refs/heads/${getBranchName(type, version)}"
        }

        // list all tags and branches
        val cmd: LsRemoteCommand = Git.lsRemoteRepository()
        remoteService.addCredentialsToCmd(cmd)
        val refs = cmd.setRemote(localService.remoteUrl).setHeads(true).setTags(true).call()

        var rv = false
        refs.forEach { r: Ref ->  rv = rv || path == r.name }
        return rv
    }
}
