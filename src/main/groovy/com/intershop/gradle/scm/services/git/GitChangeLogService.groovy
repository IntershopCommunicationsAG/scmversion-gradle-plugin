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

import com.intershop.gradle.scm.extension.VersionExtension
import com.intershop.gradle.scm.services.ScmChangeLogService
import com.intershop.gradle.scm.services.ScmLocalService
import com.intershop.gradle.scm.utils.ScmKey
import com.intershop.gradle.scm.utils.ScmUser
import com.intershop.gradle.scm.version.VersionTag
import groovy.util.logging.Slf4j
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.filter.TreeFilter

@Slf4j
class GitChangeLogService extends GitRemoteService implements ScmChangeLogService {

    private VersionExtension versionExt

    GitChangeLogService(ScmLocalService sls,
                        VersionExtension versionExt,
                        ScmUser user = null,
                        ScmKey key = null) {
        super(sls, user, key)
        this.versionExt = versionExt
        type = sls.type
    }

    void createLog() {
        VersionTag pvt = null
        try {
            pvt = versionExt.getPreviousVersionTag(getTargetVersion())
        } catch(Exception ex) {
            log.warn(ex.getMessage())
        }

        if(pvt) {
            this.changelogFile.append(getHeader(versionExt.getVersionService().getPreVersion().toString(), pvt.ver.toString()))

            Iterable<RevCommit> refs = localService.client.log().addRange(getObjectId(pvt.branchObject.id), getObjectId(localService.getRevID())).call();
            refs.findAll().each { RevCommit rc ->
                this.changelogFile.append(getLineMessage(rc.getFullMessage(), rc.getName().substring(0, 8)))
                getFilesInCommit(rc)
            }
        } else {
            this.changelogFile.append(getHeader(versionExt.getVersionService().getPreVersion().toString(), 'not available'))
        }
        this.changelogFile.append(getFooter())
    }

    private void getFilesInCommit(RevCommit commit) {
        DiffFormatter diffFmt = new DiffFormatter(  new BufferedOutputStream(System.out) )
        diffFmt.setRepository(localService.repository)
        diffFmt.setPathFilter(TreeFilter.ANY_DIFF)
        diffFmt.setDetectRenames(true)

        RevWalk rw = new RevWalk(localService.repository)
        rw.parseHeaders(commit.getParent(0))

        RevTree a = commit.getParent(0).getTree()
        RevTree b = commit.getTree()

        diffFmt.scan(a, b).each { DiffEntry e ->

            switch (e.getChangeType()) {
                case DiffEntry.ChangeType.ADD:
                    this.changelogFile.append(getLineChangedFile(e.getNewPath(), 'A'))
                    break
                case DiffEntry.ChangeType.DELETE:
                    this.changelogFile.append(getLineChangedFile(e.getOldPath(), 'D'))
                    break
                case DiffEntry.ChangeType.MODIFY:
                    this.changelogFile.append(getLineChangedFile(e.getNewPath(), 'M'))
                    break
                case DiffEntry.ChangeType.COPY:
                    this.changelogFile.append(getLineChangedFile("${e.getOldPath()} ->\n${e.getNewPath()} (${e.getScore()})", 'C'))
                    break
                case DiffEntry.ChangeType.RENAME:
                    this.changelogFile.append(getLineChangedFile("${e.getOldPath()} ->\n${e.getNewPath()} (${e.getScore()})", 'R'))
                    break
            }
        }
    }
}
