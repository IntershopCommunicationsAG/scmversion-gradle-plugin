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
package com.intershop.gradle.scm.task

import com.intershop.gradle.scm.extension.ScmExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * This is the implementation of Gradle
 * task to show the calculated version
 * from SCM.
 */
open class ShowVersion: DefaultTask() {

    init {
        outputs.upToDateWhen { false }

        description = "Prints current project version extracted from SCM."
        group = "Release Version Plugin"
    }

    /**
     * Implementation of the task action.
     */
    @TaskAction
    fun show() {
        val versionConfig = project.extensions.getByType(ScmExtension::class.java).version

        val version = versionConfig.version
        project.logger.debug("Version is {}", version)

        println("""
            |----------------------------------------------
            |        Project version: $version
            |----------------------------------------------""".trimMargin())
    }
}
