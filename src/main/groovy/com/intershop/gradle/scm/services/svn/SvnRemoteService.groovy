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
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.AbstractBranchFilter
import groovy.util.logging.Slf4j
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNWCUtil
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver
import org.tmatesoft.svn.core.wc2.SvnList
import org.tmatesoft.svn.core.wc2.SvnOperationFactory
import org.tmatesoft.svn.core.wc2.SvnTarget

@Slf4j
class SvnRemoteService {

    protected SvnLocalService localService = null

    /*
    * SVN operations factory
    */
    protected final SvnOperationFactory svnOpFactory

    /**
     * Constructor
     *
     * @param sls  ScmLocalService contains all local information
     * @param user User credentials
     */
    SvnRemoteService(ScmLocalService sls,
                     ScmUser user) {

        if (! sls instanceof SvnLocalService) {
            throw new ScmException("Info service is not an instance of SvnLocalService")
        } else {
            this.localService = (SvnLocalService) sls
        }

        // set credentials (only username/password or "default"[svn configuration] are supported)
        svnOpFactory = new SvnOperationFactory()
        if(user.name && user.password) {
            log.debug('Add username / password authentication manager')
            svnOpFactory.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager(user.name, user.password.toCharArray()))
        } else {
            log.debug('Add default authentication manager')
            svnOpFactory.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager())
        }
    }

    /**
     * Map with rev ids and assigned tag names.
     *
     * @param branchFilter  Filters branch names
     * @return  map with revision and object with all information about the assoziated branch
     */
    public Map<String, BranchObject> getTagMap(AbstractBranchFilter branchFilter) {
        return getBranchMap(branchFilter, BranchType.tag)
    }

    /**
     * Map with rev ids and assigned branch names.
     *
     * @param branchFilter  Filters branch names
     * @param type          Type of the branch (tag/branch)
     * @return  map with revision and object with all information about the assoziated branch
     */
    protected Map<String, BranchObject> getBranchMap(AbstractBranchFilter branchFilter, BranchType type) {
        final SvnList list = svnOpFactory.createList()
        list.setDepth(SVNDepth.IMMEDIATES)
        list.addTarget(SvnTarget.fromURL(getVersionBranch(type)))

        Map<String, BranchObject> branches = [:]

        // receive all 'branches' (a tag is only a special branch)
        list.setReceiver(new ISvnObjectReceiver<SVNDirEntry>() {
            public void receive(SvnTarget target, SVNDirEntry object) throws SVNException {
                final String bname = object.getRelativePath()
                String version = branchFilter.getVersionStr(bname)

                log.debug('Check branch name for version {}', bname)
                if(version != null && ! version.isEmpty()) {
                    log.debug('Branch named added to list {}', bname)
                    branches.put(Long.toString(object.getRevision()), new BranchObject(Long.toString(object.getRevision()), version, bname))
                }
            }
        })
        list.run()

        return branches
    }

    /**
     * Returns the SVNURL of a special branch type based on the root directory.
     *
     * @param type branch type
     * @return SVN url for the branch type
     */
    protected SVNURL getVersionBranch(BranchType type) {
        switch (type) {
            case BranchType.branch:
                return localService.projectRootSvnUrl.appendPath('branches',false)
                break
            case BranchType.tag:
                return localService.projectRootSvnUrl.appendPath('tags',false)
                break
        }
        return localService.projectRootSvnUrl.appendPath('trunk', false)
    }
}
