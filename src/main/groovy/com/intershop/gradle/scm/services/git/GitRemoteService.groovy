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

import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.utils.BranchObject
import com.intershop.gradle.scm.utils.ScmException
import com.intershop.gradle.scm.utils.ScmKey
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.AbstractBranchFilter
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.FetchCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.*
/**
 * Remote Scm service for Git
 */
@CompileStatic
@Slf4j
class GitRemoteService {

    protected GitLocalService localService = null

    /**
     * Credential configuration
     */
    protected final CredentialsProvider credentials
    protected final SshConnector sshConnector

    /**
     * Returns true if are credentials configured
     */
    protected final boolean remoteConfigAvailable

    /**
     * Construcutor
     * @param sls  ScmLocalService contains all local information
     * @param user User credentials
     * @param key  Key credentials
     */
    GitRemoteService(ScmLocalService sls,
                     ScmUser user = null,
                     ScmKey key = null) {

        if (! sls instanceof GitLocalService) {
            throw new ScmException("Info service is not an instance of GitLocalService")
        } else {
            this.localService = (GitLocalService) sls
        }

        log.debug('Configure authentication')

        if(localService.remoteUrl) {
            if (localService.remoteUrl.startsWith('http') && user && user.name && user.password) {
                log.debug('User name {} and password is used.', user.name)
                credentials = new UsernamePasswordCredentialsProvider(user.name, user.password)
                remoteConfigAvailable = true
            } else if (localService.remoteUrl.startsWith('git@') && key && key.file.exists()) {
                log.debug('ssh connector is used with key {}.', key.file.absolutePath)
                sshConnector = new SshConnector(key)
                remoteConfigAvailable = true
            } else {
                remoteConfigAvailable = false
            }
        } else {
            remoteConfigAvailable = false
        }
    }

    /**
     * Calculates the rev object from revision id string
     *
     * @param rev
     * @return rev object from the Git repository
     */
    protected RevObject getObjectId(String rev) {
        ObjectId id = localService.repository.resolve(rev)
        RevWalk rw = new RevWalk(localService.getClient().repository)
        return rw.parseAny(id)
    }

    /**
     * Map with rev ids and assigned tag names.
     */
    Map<String, BranchObject> getTagMap(AbstractBranchFilter branchFilter) {
        //specify return value
        Map<String, BranchObject> rv = [:]

        // fetch all tags from repo
        if(remoteConfigAvailable) {
            fetchTagsCmd()
        }
        //specify walk
        final RevWalk walk = new RevWalk(localService.getClient().repository)

        //check tags and calculate
        localService.repository.getRefDatabase().getRefsByPrefix(Constants.R_TAGS).each { Ref ref ->
            String tagName = ref.name.substring(Constants.R_TAGS.length())
            String version = branchFilter.getVersionStr(tagName)
            if(version) {
                RevCommit rc = walk.parseCommit(ref.objectId)
                rv.put(ObjectId.toString(rc), new BranchObject(ObjectId.toString(rc), version, tagName))
            }
        }

        walk.dispose()
        return rv
    }

    /**
     * fetch all tag information from remote branch
     * remote connection is necessary
     */
    protected void fetchTagsCmd() {
        try {
            // fetch tags
            FetchCommand cmd = localService.getClient().fetch()
            cmd.remote = 'origin'
            cmd.tagOpt = TagOpt.FETCH_TAGS
            cmd.refSpecs = [Transport.REFSPEC_TAGS]
            addCredentialsToCmd(cmd)
            cmd.call()
        } catch(InvalidRemoteException nrex) {
            log.warn('No remote repository is available! {}', nrex.getMessage())
        } catch(TransportException tex) {
            tex.printStackTrace()
            log.warn('It was not possible to fetch all tags. Please check your credential configuration.', tex)
        }
    }

    /**
     * add credentials to the git command.
     *
     * @param cmd git command
     */
    protected void addCredentialsToCmd(TransportCommand cmd) {
        // add credentials to command
        if (credentials) {
            cmd.setCredentialsProvider(credentials)
        } else if (sshConnector) {
            cmd.setTransportConfigCallback(new TransportConfigCallback() {
                void configure(Transport transport) {
                    SshTransport sshTransport = (SshTransport) transport
                    sshTransport.setSshSessionFactory(sshConnector)
                }
            })
        }
    }
}
