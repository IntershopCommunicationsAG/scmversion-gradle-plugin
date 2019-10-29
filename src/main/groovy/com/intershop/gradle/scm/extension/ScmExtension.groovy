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
package com.intershop.gradle.scm.extension

import com.intershop.gradle.scm.utils.PrefixConfig
import com.intershop.gradle.scm.utils.ScmKey
import com.intershop.gradle.scm.utils.ScmType
import com.intershop.gradle.scm.utils.ScmUser
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Action
import org.gradle.api.Project
/**
 * Extension for all SCM based plugins.
 */

@CompileStatic
@Slf4j
class ScmExtension extends AbstractExtension{

    // scm user name / token (git,svn)
    public final static String USERNAME_ENV = 'SCM_USERNAME'
    public final static String USERNAME_PRJ = 'scmUserName'

    // scm password (git,svn)
    public final static String PASSWORD_ENV = 'SCM_PASSWORD'
    public final static String PASSWORD_PRJ = 'scmUserPasswd'

    // scm key file (git)
    public final static String KEYFILE_ENV = 'SCM_KEYFILE'
    public final static String KEYFILE_PRJ = 'scmKeyFile'

    // scm passphrase (git)
    public final static String PASSPHRASE_ENV = 'SCM_KEYPASSPHRASE'
    public final static String PASSPHRASE_PRJ = 'scmKeyPassphrase'

    /**
     * <p>SCM user</p>
     *
     * <p>Can be configured/overwritten with environment variables SCM_USERNAME,
     * SCM_PASSWORD; java environment SCM_USERNAME, SCM_PASSWORD or project
     * variables scmUserName, scmUserPasswd</p>
     *
     * <pre>
     * {@code
     *      user {
     *          name = ''
     *          password = ''
     *      }
     * }
     * </pre>
     */
    ScmUser user

    /**
     * <p>SCM key</p>
     *
     * <p>Can be configured/overwritten with environment variables SCM_KEYFILE,
     * SCM_KEYPASSPHRASE; java environment SCM_KEYFILE, SCM_KEYPASSPHRASE or project
     * variables scmKeyFile, scmKeyPassphrase</p>
     * <pre>
     * {@code
     *      key {
     *          file (File)
     *          passphrase = ''
     *      }
     * }
     * </pre>
     */
    ScmKey key

    /**
     * prefixes for branches and tags
     * <pre>
     * {@code
     *      prefixes {
     *          stabilizationPrefix = 'SB'
     *          featurePrefix = 'FB'
     *          tagPrefix = 'RELEASE'
     *
     *          prefixSeperator = '_'
     *      }
     * }
     * </pre>
     */
    PrefixConfig prefixes

    /**
     * This extension contains settings for version calculation and
     * read properties for the current version and previous version.
     */
    VersionExtension version

    /**
     * This extension contains settings for change log configuration.
     */
    ChangeLogExtension changelog

    /*
     ScmType
     */
    private ScmType scmType

    ScmExtension(Project p) {
        super(p)

        initScmType()

        // default prefixes configuration
        this.prefixes = new PrefixConfig()

        // default version configuration
        this.version = new VersionExtension(p)

        // default changelog configuration
        this.changelog = new ChangeLogExtension(p)

        // authentication configuration
        setUser(createUserConfig())
        setKey(createKeyConfig())
    }

    /**
     * initialize the value for scmType
     */
    private void initScmType() {
        File gitDir = new File(project.rootDir, '.git')
        if (gitDir.exists() && gitDir.isDirectory()) {
            scmType = ScmType.git
        }

        if (!scmType) {
            scmType = ScmType.file
        }
    }

    /**
     * Getter for scmType
     * @return the scmType object for the detected SCM
     */
    ScmType getScmType() {
        return scmType
    }

    /**
     * Prefix configuration for branches and tags
     *
     * @param prefix configuration (see PrefixConfig)
     */
    void prefixes(Closure c) {
        project.configure(prefixes, c)
    }

    /**
     * Prefix configuration for branches and tags for Java/Kotlin
     *
     * @param prefix configuration (see PrefixConfig)
     */
    void prefixes(Action<? super PrefixConfig> prefixconfig) {
        prefixconfig.execute(prefixes)
    }

    /**
     * SCM user for authentication
     *
     * @param scm user (see ScmUser)
     */
    void user(Closure c) {
        project.configure(user, c)
    }

    /**
     * SCM user for authentication for Java/Kotlin
     *
     * @param scm user (see ScmUser)
     */
    void user(Action<? super ScmUser> userconfig) {
        userconfig.execute(user)
    }

    /**
     * SCM key configuration for authentication
     *
     * @param scm user (see ScmKey)
     */
    void key(Closure c) {
        project.configure(key, c)
    }

    /**
     * SCM key configuration for authentication for Java/Kotlin
     *
     * @param scm user (see ScmKey)
     */
    void key(Action<? super ScmKey> keyconfig) {
        keyconfig.execute(key)
    }

    /**
     * Version extenions with configuration
     *
     * @param version extension (VersionExtension)
     */
    void version(Closure c) {
        project.configure(version, c)
    }

    /**
     * Version extenions with configuration for Java/Kotlin
     *
     * @param version extension (VersionExtension)
     */
    void version(Action<? super VersionExtension> versionconfig) {
        versionconfig.execute(version)
    }

    /**
     * Changelog extenions with configuration
     *
     * @param changelog extension (ChangelogExtension)
     */
    void changelog(Closure c) {
        project.configure(changelog, c)
    }

    /**
     * Changelog extenions with configuration for Java/Kotlin
     *
     * @param changelog extension (ChangelogExtension)
     */
    void changelog(Action<? super ChangeLogExtension> changelogconfig) {
        changelogconfig.execute(changelog)
    }

    /**
     * initialize SCM user configuration from the environment
     */
    private ScmUser createUserConfig() {
        log.debug("Scm User will be initialized")

        ScmUser user = new ScmUser()
        user.setName(getVariable(USERNAME_ENV, USERNAME_PRJ, System.getProperty('user.name')))
        user.setPassword(getVariable(PASSWORD_ENV, PASSWORD_PRJ, ''))

        if(user.name) {
            log.debug('User name is {}', user.name)
            return user
        } else {
            log.debug('User is not set!')
            return null
        }
    }

    /**
     * initialize SCM key configuration from the environment
     */
    private ScmKey createKeyConfig() {
        ScmKey keyConfig = null

        String keyPath = getVariable(KEYFILE_ENV, KEYFILE_PRJ, null)
        File key = keyPath && (new File(keyPath)).exists() ? new File(keyPath) : new File(System.getProperty("user.home"), '.ssh/id_dsa')

        if (key != null && key.exists() && key.isFile() && key.canRead()) {
            keyConfig = new ScmKey()
            keyConfig.setFile(key)
            String passphrase = getVariable(PASSPHRASE_ENV, PASSPHRASE_PRJ, '')
            if (passphrase) {
                keyConfig.setPassphrase(passphrase)
            }
        }

        return keyConfig
    }
}
