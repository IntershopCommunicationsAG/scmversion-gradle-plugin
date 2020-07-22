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
package com.intershop.gradle.scm.services.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.LogCommand
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.utils.BranchType

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class GitLocalService extends ScmLocalService{

    /*
     * Git repo object for all operations
     */
    private final Repository gitRepo
    /*
     * Git client for all operations
     */
    private final Git gitClient

    /*
     * Remote URL of the project
     */
    private final String remoteUrl

    /**
     * This constructs a SCM info service. It contains all necessary information from
     * the working copy without remote access to the SCM. It must be implemented for
     * supported SCMs.
     *
     * @param projectDir
     * @param prefixes
     */
    GitLocalService(File projectDir, ScmExtension scmExtension) {
        super(projectDir, scmExtension)

        gitRepo = new RepositoryBuilder()
                .readEnvironment()
                .findGitDir(projectDir)
                .build()
        gitClient = new Git(gitRepo)

        branchName = gitRepo.getBranch()
        log.info('Initial branch name is {}', branchName)

        if(branchName == 'master') {
            branchType = BranchType.trunk
        } else {
            def mfb = branchName =~ /${prefixes.getFeatureBranchPattern()}/
            def mhb = branchName =~ /${prefixes.getHotfixBranchPattern()}/
            def mbb = branchName =~ /${prefixes.getBugfixBranchPattern()}/
            def msb = branchName =~ /${prefixes.getStabilizationBranchPattern()}/

            if(branchName.equals(getRevID())) {
                branchType = BranchType.detachedHead
            } else if(mfb.matches() && mfb.count == 1) {
                branchType = BranchType.featureBranch
                setFeatureBranchName((mfb[0] as List)[(mfb[0] as List).size() - 1].toString())
            } else if(mhb.matches() && mhb.count == 1) {
                branchType = BranchType.hotfixbBranch
                setFeatureBranchName((mhb[0] as List)[(mhb[0] as List).size() - 1].toString())
            } else if(mbb.matches() && mbb.count == 1) {
                branchType = BranchType.bugfixBranch
                setFeatureBranchName((mbb[0] as List)[(mbb[0] as List).size() - 1].toString())
            } else if(msb.matches() && msb.count == 1) {
                branchType = BranchType.branch
                def branchCheck = branchName =~ /^\d+(\.\d+)?(\.\d+)?(\.\d+)?(-.*)?/
                withVersion = branchCheck.matches()
            } else {
                branchType = BranchType.featureBranch
                setFeatureBranchName(branchName)
            }
        }
        log.info('Initial branch type is {}', branchType)
        
        String tn = checkHeadForTag()
        if(branchType == branchType.detachedHead && tn) {
            branchType = BranchType.tag
            branchName = tn
            log.info('Updated branch name is {}', branchName)
            log.info('Updated branch type is {}', branchType)
        }

        Config config = gitRepo.getConfig()
        remoteUrl = config.getString('remote', 'origin', 'url')

        log.info('Remote URL is {}', remoteUrl)

        try {
            LogCommand logLocal = gitClient.log()
            List<String> revsLocal = []
            Iterable<RevCommit> logsLocal = logLocal.call()
            logsLocal.each { RevCommit rev ->
                revsLocal.add(rev.toString())
            }
        } catch (Exception ex) {
            log.warn('No repo info available! {}', ex.getMessage())
        }

        Status status = gitClient.status().call()

        changed = status.untracked.size() > 0 || status.uncommittedChanges.size() > 0 || status.removed.size() > 0 || status.added.size() > 0 || status.changed.size() > 0 || status.modified.size() > 0

        if(log.infoEnabled && changed) {
            log.info('There are local changes on the repository.')
            if(status.untracked.size() > 0) {
                status.untracked.each {
                    log.info('GIT: This file is not indexed {}', it)
                }
                status.removed.each {
                    log.info('GIT: This file is deleted {}', it)
                }
                status.added.each {
                    log.info('GIT: This file is added {}', it)
                }
                status.changed.each {
                    log.info('GIT: This file is changed {}', it)
                }
                status.modified.each {
                    log.info('GIT: This file is modified {}', it)
                }
                status.uncommittedChanges.each {
                    log.info('GIT: This file is uncommitted {}', it)
                }
            }
        }

        if(! changed && branchType == BranchType.detachedHead) {
            log.info('Repo is in detached mode! Create a tag on {}.', branchName)
        }
    }

    private String checkHeadForTag() {
        String rvTagName = ''
        RevWalk rw = new RevWalk(repository)

        String headRevID = getRevID()
        List<Ref> tagRefs = gitRepo.getRefDatabase().getRefsByPrefix(Constants.R_TAGS) 
        tagRefs.each {Ref ref ->
            if(ObjectId.toString(rw.parseCommit(ref.objectId).id) == headRevID) {
                rvTagName = ref.name.substring(Constants.R_TAGS.length())
            }
        }
        rw.dispose()
        return rvTagName
    }

    /**
     * Access for the GitRepo Object
     * @return
     */
    Repository getRepository() {
        return gitRepo
    }

    /**
     * Access for the GitClient Object
     * @return
     */
    Git getClient() {
        return gitClient
    }

    /**
     * It returns the remote url, calculated from the properties of the working copy (read only).
     *
     * @return remote url
     */
    @Override
    String getRemoteUrl() {
        return remoteUrl
    }

    /**
     * The revision id from the working copy (read only).
     *
     * @return revision id
     */
    @Override
    String getRevID() {
        ObjectId id = gitRepo.resolve(Constants.HEAD)
        String rv = ''

        if(id) {
            rv = id.getName()
        }

        return rv
    }
}
