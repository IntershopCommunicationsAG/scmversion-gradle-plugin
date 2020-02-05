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
package com.intershop.gradle.scm.services

import com.intershop.gradle.scm.utils.BranchType
import com.intershop.gradle.scm.utils.PrefixConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * This is the container of all information from an existing
 * working copy without any access to the remote location of the project.
 */
abstract class ScmLocalService(val projectDir: File,
                               val prefixes: PrefixConfig) {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(this::class.java.name)
        const val BRANCH_PATTERN = "^\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?(-.*)?"
    }

    private var featureBranchNameInternal: String = ""

    /**
     * The base branch type of the current working copy.
     */
    var branchType: BranchType = BranchType.BRANCH
        protected set

    /**
     * The base (stabilization) branch name of the current working copy
     */
    abstract val branchName: String

    /**
     * The base feature branch name of the current working copy
     */
    var featureBranchName: String
        get() = featureBranchNameInternal
        set(value) {
            val result = Regex(BRANCH_PATTERN).matchEntire(value)

            featureBranchNameInternal = if(result?.groupValues != null && result.groupValues.size > 3) {
                result.groupValues.last().substring(1)
            } else {
                value
            }

            branchWithVersion = featureBranchNameInternal != value
        }

    /**
     * This is true, if the local working copy changed.
     *
     * @property changed
     */
    abstract val changed: Boolean

    /**
     * This is true, if the local working copy is a commit wihtout other changes.
     * It is only used with a Git repository.
     *
     * @property commitOnly
     */
    var commitOnly: Boolean = true

    /**
     * This is true, if the feature branch contains a version.
     *
     * @property branchWithVersion
     */
    var branchWithVersion: Boolean = false

    /**
     * It returns the remote url, calculated from the properties of the working copy (read only).
     *
     * @property remoteUrl remote url
     */
    abstract val remoteUrl: String

    /**
     * The revision id from the working copy (read only).
     *
     * @property revID revision id
     */
    abstract val revID: String
}