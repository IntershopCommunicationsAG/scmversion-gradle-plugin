/*
 * Copyright 2015 Intershop Communications AG.
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
package com.intershop.gradle.scm.services.svn

import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.services.ScmVersionService
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.ReleaseFilter
import com.intershop.gradle.scm.version.ScmBranchFilter
import com.intershop.gradle.scm.version.ScmVersionObject
import com.intershop.gradle.scm.version.VersionTag
import com.intershop.release.version.Version
import com.intershop.release.version.VersionParser
import groovy.util.logging.Slf4j
import org.tmatesoft.svn.core.*
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc2.*

/**
 * This is the container for the remote access to the used SCM of a project.
 * It calculates the version and has methods to create a branch, a tag or
 * move the working copy to a special version.
 */
@Slf4j
class SvnVersionService extends SvnRemoteService implements ScmVersionService {

    /**
     * Constructs a valid remote client for SCM access.
     *
     * @param infoService information from the working copy
     * @param versionBranchType branch type which is used for version calculation
     * @param versionType version type - three or four version digits
     * @param patternDigits Number of digits used for the search pattern of valid branches
     * with version information.
     */
    SvnVersionService(ScmLocalService sls,
                      ScmUser user) {
        super(sls, user)
        localService = sls
    }

    /**
     * Returns an object from the SCM with additional information.
     *
     * @return version object from scm
     */
    public ScmVersionObject getVersionObject() {
        ScmBranchFilter tagFilter = getBranchFilter(BranchType.tag)
        ScmBranchFilter branchFilter = getBranchFilter(BranchType.branch)

        // the working copy is a tag
        if(((SvnLocalService)localService).branchType == BranchType.tag) {
            log.debug('Version information is taken from tag {}', ((SvnLocalService)localService).branchName)
            String v = tagFilter.getVersionStr(((SvnLocalService)localService).branchName)
            if(v) {
                return new ScmVersionObject(((SvnLocalService)localService).branchName, Version.forString(v, versionExt.getVersionType()),
                        ((SvnLocalService)localService).changed && checkForCommits())
            } else {
                throw new ScmException('It is not possible to extract the version of the tag.')
            }
        }

        log.debug('Version information is calculated for branches')
        // the working copy is a branch or trunk
        final SvnList list = svnOpFactory.createList()
        list.setDepth(SVNDepth.IMMEDIATES)
        list.addTarget(SvnTarget.fromURL(getVersionBranch(versionExt.versionBranchType)))

        def branches = [:]

        // the source folder (branches/tags) with the folders with versions is used
        // for the correct configuration of the version filter
        ScmBranchFilter tf = versionExt.getVersionBranchType() == BranchType.branch ? branchFilter : tagFilter

        // receive all 'branches' (a tag is only a special branch)
        list.setReceiver(new ISvnObjectReceiver<SVNDirEntry>() {
            public void receive(SvnTarget target, SVNDirEntry object) throws SVNException {
                final String name = object.getRelativePath()
                log.debug('Check branch name for version {}', name)
                String version = tf.getVersionStr(name)
                if(version) {
                    log.debug('Branch named added to list {}', name)
                    branches.put(Version.forString(version, versionExt.getVersionType()), name)
                }
            }
        })
        list.run()

        //find the last / largest, because there is no real order in svn
        List l = branches.keySet().sort(true).reverse(true)
        if(l.size() > 0 && l.get(0)) {
            log.debug('Version found and latest will be used.')
            // check for changes
            boolean remoteChanges = false
            if (!localService.changed) {
                log.debug('Content of project was not changed locally.')
                remoteChanges = isChanged(branches.get(l.get(0)), versionExt.getVersionBranchType())
            }
            log.debug('Create ScmVersionObject for {},{},{}', branches.get(l.get(0)).toString(), (Version)l.get(0),remoteChanges || localService.changed)
            ScmVersionObject rv = new ScmVersionObject(branches.get(l.get(0)).toString(), (Version)l.get(0), remoteChanges || localService.changed)
            return rv
        }

        if(localService.branchType == BranchType.branch || localService.branchType == BranchType.featureBranch) {
            log.debug('Version is taken from the branch')

            String versionString = getBranchFilter(localService.branchType).getVersionStr(localService.branchName)

            if(versionString) {
                ScmVersionObject rv = new ScmVersionObject(localService.branchName, VersionParser.parseVersion(versionString, versionExt.getVersionType()), true)
                return rv
            }
        }

        return getFallbackVersion()
    }

    public Map<Version, VersionTag> getVersionTagMap() {
        Map<String, BranchObject> branchMap = this.getTagMap(new ReleaseFilter(localService.prefixes, getPreVersion()))

        Map<Version, VersionTag> versionTags = [:]
        branchMap.each {key, bo ->
            Version v = Version.valueOf(bo.version)
            versionTags.put(v, new VersionTag(v, bo))
        }

        return versionTags
    }

    /**
     * Identification of changes between the latest version branch
     * and the current current working directory.
     * @param branchName latest branch name with version information
     * @param type branch type
     */
    private boolean isChanged(String branchName, BranchType type) {
        int changes = 0
        SvnDiffSummarize svnDiff = svnOpFactory.createDiffSummarize()
        svnDiff.depth = SVNDepth.INFINITY

        svnDiff.setSources(SvnTarget.fromFile(localService.projectDir, SVNRevision.create(Long.parseLong(localService.getRevID()))),
                SvnTarget.fromURL(getVersionBranch(type).appendPath(branchName, false), SVNRevision.HEAD))

        svnDiff.setReceiver(new ISvnObjectReceiver<SvnDiffStatus>() {
            public void receive(SvnTarget target, SvnDiffStatus object) throws SVNException {
                log.debug("{} was changed", object.path)
                ++changes
            }
        })
        svnDiff.run()
        return changes > 0
    }

    /**
     * Moves the working copy to a specified version
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the working after the move
     */
    public String moveTo(String version, boolean featureBranch) {
        log.debug('svn checkout {}', version)

        SVNURL url = null
        if(checkBranch(BranchType.tag ,version)) {
            url = getVersionBranch(BranchType.tag).appendPath(getBranchName(BranchType.tag, version), false)
        } else if(checkBranch(featureBranch ? BranchType.featureBranch : BranchType.branch, version)) {
            url = getVersionBranch(BranchType.branch).appendPath(getBranchName(featureBranch ? BranchType.featureBranch : BranchType.branch, version),false)
        } else {
            throw new ScmException("Version '${version}' does not exist")
        }

        log.debug('Move to {}', url.toString())

        SvnSwitch switchCmd = svnOpFactory.createSwitch()
        switchCmd.setSingleTarget(SvnTarget.fromFile(localService.getProjectDir()))
        switchCmd.setSwitchTarget(SvnTarget.fromURL(url, SVNRevision.HEAD))


        log.debug('Specified target is {}', switchCmd.getSwitchTarget())

        long rev = switchCmd.run()
        return Long.toString(rev)
    }

    /**
     * Creates a tag with the specified version.
     *
     * @param version
     * @return the revision id of the tag
     */
    public String createTag(String version, String revid = localService.getRevID()) {
        if ( checkBranch(BranchType.tag, version) ) {
            throw new ScmException("Tag for ${version} exists!")
        }

        String tagName = getBranchName(BranchType.tag, version)

        return copyClient(SvnCopySource.create(SvnTarget.fromURL(((SvnLocalService)localService).getRemoteSvnUrl()), SVNRevision.create(Long.parseLong(revid))),
                          SvnTarget.fromURL(getVersionBranch(BranchType.tag).appendPath(tagName,false)), 'tag')
    }

    /**
     * Creates a branch with the specified version.
     *
     * @param version
     * @param featureBranch true, if this is a version of a feature branch
     * @return the revision id of the branch
     */
    public String createBranch(String version, boolean featureBranch, String revid = localService.getRevID()) {

        if (checkBranch(featureBranch ? BranchType.featureBranch : BranchType.branch, version) ) {
            throw new ScmException("Branch for ${version} exists!")
        }

        String branchName = getBranchName(featureBranch ? BranchType.featureBranch : BranchType.branch, version)

        return copyClient(SvnCopySource.create(SvnTarget.fromURL(((SvnLocalService)localService).getRemoteSvnUrl()), SVNRevision.create(Long.parseLong(revid))),
                          SvnTarget.fromURL(getVersionBranch(BranchType.branch).appendPath(branchName,false)), 'Branch')
    }

    /**
     * Returns true, if the specified release version is available.
     *
     * @param version
     * @return true, if the specified release version is available
     */
    public boolean isReleaseVersionAvailable(String version) {
        return checkBranch(BranchType.tag, version)
    }

    /**
     * Inspection of the branch with a special type for a version.
     *
     * @param type
     * @param version
     * @return true if the branch exists
     */
    private boolean checkBranch(BranchType type, String version) {
        SVNURL svnURL = null
        String name = getBranchName(type, version)

        if(type == BranchType.featureBranch || type == BranchType.branch) {
            svnURL = getVersionBranch(BranchType.branch).appendPath(name,false)
        } else {
            svnURL = getVersionBranch(BranchType.tag).appendPath(name,false)
        }

        SVNRepository repo = SVNRepositoryFactory.create(svnURL)
        repo.setAuthenticationManager(svnOpFactory.getAuthenticationManager())
        SVNNodeKind nodeKind = repo.checkPath( "" ,  -1 )

        return  nodeKind == SVNNodeKind.DIR || nodeKind == SVNNodeKind.FILE
    }

    /**
     * SVN copy
     *
     * @param source
     * @param target
     * @param type
     * @return revision id of the copy
     */
    private String copyClient(SvnCopySource source, SvnTarget target, String type) {
        log.debug("initialize client")
        final SvnRemoteCopy remoteCopy = svnOpFactory.createRemoteCopy()
        remoteCopy.addCopySource(source)
        remoteCopy.setCommitMessage("${type} created by gradle plugin")
        remoteCopy.setFailWhenDstExists(true)
        remoteCopy.setMakeParents(true)
        remoteCopy.setSingleTarget(target)
        SVNCommitInfo info = remoteCopy.run()
        if(info.errorMessage) {
            log.error("Branch creation failed!")
            throw new ScmException("Branching failed with ${info.errorMessage}.")
        }
        return Long.toString(info.newRevision)
    }

    /**
     * SVN check for commits after tag creation ...
     */
    private boolean checkForCommits() {
        final SvnLog svnLog = svnOpFactory.createLog()
        int changes = 0

        svnLog.stopOnCopy = true
        svnLog.setSingleTarget(SvnTarget.fromFile(localService.projectDir))
        svnLog.setReceiver(new ISvnObjectReceiver<SVNLogEntry>() {
            public void receive(SvnTarget target, SVNLogEntry logEntry) throws SVNException {
                log.info('Changes on branch detected: {}', logEntry.getRevision())
                ++changes
            }
        })
        svnLog.setRevisionRanges(Collections.singleton(SvnRevisionRange.create(SVNRevision.create(0), SVNRevision.HEAD)))
        svnLog.run()

        //Initial commit is always available
        return (changes > 1)
    }


}
