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
package com.intershop.gradle.scm

import com.intershop.gradle.scm.extension.ScmExtension
import com.intershop.gradle.scm.task.*
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
/**
 * <p>This plugin will apply the scmPlugin.</p>
 *
 * <p>It will calculate the version from the used
 * SCM. It support Git or Subversion.</p>
 * <p>It adds furthermore some tasks for the release and
 * SCM management of this project based on the version:
 *
 * <p><b>tag</b> - creates a tag on the SCM</p>
 * <p><b>branch</b> - creates a (stabilization or feature) branch on the SCM</p>
 * <p><b>toVersion</b> - move the working copy to the specified version (tag or branch)</p>
 * <p><b>release</b> - create a version tag on the SCM and move the working copy to this version</p>
 * <p><b>showVersion</b> - shows the calculated version</p>
 */
@CompileStatic
class ScmVersionPlugin implements Plugin<Project> {

    /**
     * Name of the extension
     */
    public static final String SCM_EXTENSION = 'scm'


    // target version property name
    public final static String TARGETVERSION_ENV = 'TARGET_VERSION'
    public final static String TARGETVERSION_PRJ = 'targetVersion'

    // change log file
    public final static String CHANGELOG_ENV = 'CHANGELOG_FILE'
    public final static String CHANGELOG_PRJ = 'changelogFile'

    /**
     * Task namen
     */
    public static final String SHOW_VERSION_TASK = 'showVersion'
    public static final String TO_VERSION_TASK = 'toVersion'
    public static final String CREATE_TAG_TASK = 'tag'
    public static final String CREATE_BRANCH_TASK = 'branch'
    public static final String RELEASE_TASK = 'release'
    public static final String CHANGELOG_TASK = 'changelog'

    private ScmExtension scmExtension

    /**
     * Creates the extension and tasks of this plugin.
     *
     * @param project
     */
    @Override
    void apply(Project project) {
        //disable gnome-keyring, because this is currently not working with gradle and jna
        System.setProperty('svnkit.library.gnome-keyring.enabled', 'false')

        // Create Extension
        scmExtension = project.extensions.findByType(ScmExtension) ?: project.extensions.create(SCM_EXTENSION, ScmExtension, project)

        scmExtension.changelog.setTargetVersion(scmExtension.changelog.getVariable(TARGETVERSION_ENV, TARGETVERSION_PRJ, ''))

        String changeLogFile = scmExtension.changelog.getVariable(CHANGELOG_ENV, CHANGELOG_PRJ, '')
        scmExtension.changelog.setChangelogFile( changeLogFile ? project.file(changeLogFile) : new File(project.getBuildDir(), 'changelog/changelog.asciidoc'))

        project.getExtensions().getExtraProperties().set('useSCMVersionConfig', true)

        // Create Tasks
        Task showVersionTask = project.tasks.maybeCreate(SHOW_VERSION_TASK, ShowVersion)
        showVersionTask.group = 'Release Version Plugin'
        showVersionTask.description = 'Prints current project version extracted from SCM.'

        Task toVersionTask = project.tasks.maybeCreate(TO_VERSION_TASK, ToVersion)
        toVersionTask.group = 'Release Version Plugin'
        toVersionTask.description = 'Moves the existing working copy to a specified version _tag_'

        Task createTagTask = project.tasks.maybeCreate(CREATE_TAG_TASK, CreateTag)
        createTagTask.group = 'Release Version Plugin'
        createTagTask.description = 'Creates an SCM tag With a specific version from the working copy'

        Task createBranchTask = project.tasks.maybeCreate(CREATE_BRANCH_TASK, CreateBranch)
        createBranchTask.group = 'Release Version Plugin'
        createBranchTask.description = 'Creates an SCM branch With a specific version from the working copy'

        Task prepareReleaseTask = project.tasks.maybeCreate(RELEASE_TASK, PrepareRelease)
        prepareReleaseTask.group = 'Release Version Plugin'
        prepareReleaseTask.description = 'Prepare a release process, create branch and/or tag, moves the working copy to the tag'

        CreateChangeLog task = project.tasks.maybeCreate(CHANGELOG_TASK, CreateChangeLog)
        task.setChangelogFile( scmExtension.changelog.getChangelogFileProvider() )
        task.setTargetVersion( scmExtension.changelog.getTargetVersionProvider() )
    }

}
