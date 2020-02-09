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


package com.intershop.gradle.scm.test.utils

import com.intershop.gradle.scm.services.git.SSHConnector
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class AbstractTaskGroovySpec extends AbstractScmGroovySpec {

    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder()

    protected void prepareGitCheckout(File testProject, String source, String branch) {
        File tempDir = tempFolder.newFolder()
        gitCheckOut(tempDir, source, branch)

        tempDir.listFiles().each {File src ->
            if(src.isDirectory()) {
                FileUtils.copyDirectory(src, new File(testProject, src.getName()))
            } else {
                if(src.name != 'build.gradle' && src.name != 'settings.gradle') {
                    FileUtils.moveFileToDirectory(src, testProject, true)
                }
            }
        }
    }

    protected void prepareTagGitCheckout(File testProject, String source, String tag) {
        File tempDir = tempFolder.newFolder()
        gitTagCheckOut(tempDir, source, 'master', tag)

        tempDir.listFiles().each {File src ->
            if(src.isDirectory()) {
                FileUtils.copyDirectory(src, new File(testProject, src.getName()))
            } else {
                if(src.name != 'build.gradle' && src.name != 'settings.gradle') {
                    FileUtils.moveFileToDirectory(src, testProject, true)
                }
            }
        }
    }


    protected void prepareGitCommitCheckout(File testProject, String source, String commitid) {
        File tempDir = tempFolder.newFolder()
        gitCommitCheckout(tempDir, source, commitid)

        tempDir.listFiles().each {File src ->
            if(src.isDirectory()) {
                FileUtils.copyDirectory(src, new File(testProject, src.getName()))
            } else {
                if(src.name != 'build.gradle' && src.name != 'settings.gradle') {
                    FileUtils.moveFileToDirectory(src, testProject, true)
                }
            }
        }
    }

    protected void prepareGitSSHCheckout(File testProject, String source, String branch, File keyFile) {
        File tempDir = tempFolder.newFolder()
        CloneCommand cmd = Git.cloneRepository()
                .setURI(source)
                .setBranch(branch)
                .setDirectory(tempDir)
                .setTransportConfigCallback(new TransportConfigCallback() {
                    void configure(Transport transport) {
                        SshTransport sshTransport = (SshTransport) transport
                        sshTransport.setSshSessionFactory(new SSHConnector(keyFile, ''))
                    }
                })
        cmd.call()

        tempDir.listFiles().each {File src ->
            if(src.isDirectory()) {
                FileUtils.copyDirectory(src, new File(testProject, src.getName()))
            } else {
                if(src.name != 'build.gradle' && src.name != 'settings.gradle') {
                    FileUtils.moveFileToDirectory(src, testProject, true)
                }
            }
        }
    }
}
