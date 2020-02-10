package com.intershop.gradle.scm.task

import org.gradle.api.DefaultTask

/**
 * Base class for all tasks of this plugin.
 */
abstract class AbstractReleaseVersionTask: DefaultTask() {

    init {
        group = "Release Version Plugin"
    }
}
