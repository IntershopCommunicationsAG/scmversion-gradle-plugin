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

import com.intershop.gradle.scm.utils.ScmKey
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.util.FS
import java.io.File

open class SSHConnector: JschConfigSessionFactory {

    private var keyFile: File? = null
    private var passphrase: String? = null
    private var jsch: JSch? = null

    /**
     * Constructs the object from the key file and a passphrase.
     *
     * @param privateKey Private key file
     * @param passphrase Passphrase for the key file
     */
    constructor( privateKey: File?,  phrase: String?) {
        keyFile = privateKey
        passphrase = phrase
    }

    /**
     * Constructs the object from the ScmKey object.
     *
     * @param key
     */
    constructor(key: ScmKey?) : this(key?.file, key?.passphrase)

    /**
     * Provide additional configuration for the session based on the host
     * information.
     *
     * @param hc
     *            host configuration
     * @param session
     *            session to configure
     */
    override fun configure(hc: OpenSshConfig.Host, session: Session) {
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
    @Throws(JSchException::class)
    override fun getJSch( hc: OpenSshConfig.Host, fs: FS): JSch? {
        if(this.jsch == null) {
            this.jsch = createJSch()
        }
        return this.jsch
    }

    private fun createJSch(): JSch {
        val jsch = JSch()

        if(keyFile != null) {
            val pp = if(passphrase != null) { passphrase!!.toByteArray() } else { null }
            jsch.addIdentity(keyFile!!.absolutePath, null, pp)
        }

        return jsch
    }
}