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

import com.intershop.gradle.scm.utils.ScmKey
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import groovy.transform.CompileStatic
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.util.FS

/**
 * Implements an SSH authentication object for JGit.
 */
@CompileStatic
class SshConnector extends JschConfigSessionFactory {

    private JSch jsch

    private File privateKey
    private String passPhrase

    /**
     * Constructs the object from the ScmKey object.
     *
     * @param key
     */
    SshConnector(ScmKey key) {
        this(key?.file, key?.passphrase)
    }

    /**
     * Constructs the object from the key file and a passphrase.
     *
     * @param privateKey Private key file
     * @param passphrase Passphrase for the key file
     */
    SshConnector(File privateKey, String passphrase) {
        this.privateKey = privateKey
        this.passPhrase = passphrase
    }

    /**
     * Provide additional configuration for the session based on the host
     * information.
     *
     * @param hc
     *            host configuration
     * @param session
     *            session to configure
     */
    @Override
    protected void configure(OpenSshConfig.Host hc, Session session) {
        session.setConfig("StrictHostKeyChecking", "no")
        session.setConfig("UserKnownHostsFile", "/dev/null")
    }

    /**
     * Obtain the JSch used to create new sessions.
     *
     * @param hc
     *            host configuration
     * @param fs
     *            the file system abstraction which will be necessary to
     *            perform certain file system operations.
     * @return the JSch instance to use.
     * @throws JSchException
     *             the user configuration could not be created.
     */
    @Override
    protected JSch getJSch(OpenSshConfig.Host hc, FS fs) throws JSchException {
        if(this.jsch == null) {
            this.jsch = createJSch()
        }
        return this.jsch
    }

    private JSch createJSch() {
        JSch jsch = new JSch()

        if(privateKey) {
            byte[] pp = passPhrase ? passPhrase.bytes : null
            jsch.addIdentity(privateKey.absolutePath, null, pp)
        }

        return jsch
    }
}
