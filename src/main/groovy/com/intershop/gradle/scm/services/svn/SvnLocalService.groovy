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

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.utils.BranchType
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions
import org.tmatesoft.svn.core.wc.SVNStatusType
import org.tmatesoft.svn.core.wc.SVNWCUtil
import org.tmatesoft.svn.core.wc2.*

import static org.tmatesoft.svn.core.SVNURL.parseURIEncoded
import static java.net.URLDecoder.decode

/**
 * This is the implementation of a ScmInfoService of Subversion based working copy.
 */
@Slf4j
@CompileStatic
class SvnLocalService extends ScmLocalService {

    /**
     * Search pattern for the branch type - trunk, branches, tags
     */
    public final static String[] branchTest = [ /(.+)\/trunk/ , /(.+)\/branches\/(.+)/ , /(.+)\/tags\/(.+)/ ]

    /**
     * SVN URL of the project
     */
    final SVNURL url

    /**
     * SVN Revsion ID
     */
    private final long revision

    /**
     * SVN URL of the project root
     */
    private SVNURL projectRootUrl



    /**
     * This constructs a SCM info service. It contains all necessary information from
     * the working copy without remote access to the SCM. It must be implemented for
     * supported SCMs.
     *
     * @param projectDir
     * @param prefixes
     */
    @CompileDynamic
    SvnLocalService(File projectDir, ScmExtension scmExtension) {
        super(projectDir, scmExtension)

        // do not change the wc
        System.setProperty("svnkit.upgradeWC", "false")

        SvnOperationFactory svnOpFactory = new SvnOperationFactory()
        log.debug('Add default authentication manager, because no special authentication is necessary for local access')
        svnOpFactory.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager())

        svnOpFactory.setOptions(new DefaultSVNOptions())

        final SvnGetInfo svnInfo = svnOpFactory.createGetInfo()
        svnInfo.setSingleTarget(SvnTarget.fromFile(projectDir))

        try {
            final SvnInfo info = svnInfo.run()

            url = info.getUrl()
            revision = info.getLastChangedRevision()

            branchTest.eachWithIndex { test, idx ->
                def m = url.toString() =~ test
                if( m.getCount()) {

                    projectRootUrl = parseURIEncoded( decode((m[0] as List)[1].toString(), "UTF-8") )

                    switch (idx) {
                        case 0:
                            branchType = BranchType.trunk
                            branchName = 'trunk'
                            break
                        case 1:
                            branchName = (m[0] as List)[2]
                            def mfb = branchName =~ /${prefixes.getFeatureBranchPattern()}/
                            def mhb = branchName =~ /${prefixes.getHotfixBranchPattern()}/
                            def mbb = branchName =~ /${prefixes.getBugfixBranchPattern()}/
                            def msb = branchName =~ /${prefixes.getStabilizationBranchPattern()}/

                            println "--- ${prefixes.getStabilizationBranchPattern()}"

                            println "--- ${msb.matches()}"
                            println "--- ${msb.count}"
                            println "--- ${msb[0].size()}"

                            if(mfb.matches() && mfb.count == 1 && (mfb[0].size() == 5 || mfb[0].size() == 6)) {
                                branchType = BranchType.featureBranch
                                featureBranchName = (mfb[0] as List)[(mfb[0] as List).size() - 1]

                            } else if(mhb.matches() && mhb.count == 1 && (mhb[0].size() == 5 || mhb[0].size() == 6)) {
                                branchType = BranchType.hotfixbBranch
                                featureBranchName = (mhb[0] as List)[(mhb[0] as List).size() - 1]

                            } else if(mbb.matches() && mbb.count == 1 && (mbb[0].size() == 5 || mbb[0].size() == 6)) {
                                branchType = BranchType.bugfixBranch
                                featureBranchName = (mbb[0] as List)[(mbb[0] as List).size() - 1]

                            } else if(msb.matches() && msb.count == 1 && (msb[0].size() == 5 || msb[0].size() == 6)) {
                                branchType = BranchType.branch

                            }  else {
                                branchType = BranchType.featureBranch
                                featureBranchName = branchName
                            }
                            break
                        case 2:
                            branchType = BranchType.tag
                            branchName = (m[0] as List)[2]
                            break
                    }
                }
            }

        } catch(SVNException ex) {
            log.error("Project directory ${projectDir.absolutePath} is not a svn copy ex.message {}", ex.getMessage())
        }

        //identify local changes
        final SvnGetStatus svnStatus = svnOpFactory.createGetStatus()
        svnStatus.setSingleTarget(SvnTarget.fromFile(projectDir))
        svnStatus.fileListHook
        svnStatus.setDepth(SVNDepth.INFINITY)
        svnStatus.reportAll = false
        svnStatus.reportIgnored = false
        svnStatus.remote = false

        svnStatus.setReceiver(new ISvnObjectReceiver<SvnStatus>() {
            void receive(SvnTarget target, SvnStatus status) throws SVNException {
                if(status.nodeStatus.getID() != SVNStatusType.STATUS_NORMAL || status.nodeStatus.getID() != SVNStatusType.STATUS_IGNORED) {
                    log.info('SVN: Local changes detected: \n {}', status.changelist)
                    changed = true
                }
            }
        })
        svnStatus.run()
    }

    /**
     * Returns the SVN url object of the project.
     * @return remote SVN url
     */
    SVNURL getRemoteSvnUrl() {
        return url
    }

    /**
     * It returns the remote url, calculated from the properties of the working copy (read only).
     *
     * @return remote url
     */
    @Override
    String getRemoteUrl() {
        return url.toString()
    }

    /**
     * Used for the SVN implementation.
     *
     * @return  the SVNURL object analog to remoteUrl
     */
    SVNURL getProjectRootSvnUrl() {
        return projectRootUrl
    }

    /**
     * Used for the SVN implementation.
     *
     * @return the remote root url of this project
     */
    String getProjectRootUrl() {
        return projectRootUrl.toString()
    }

    /**
     *
     * @return the original SVN revision id analage to revID
     */
    long getRevision() {
        return revision
    }

    /**
     * The revision id from the working copy (read only).
     *
     * @return revision id
     */
    @Override
    String getRevID() {
        return Long.toString(revision)
    }
}
