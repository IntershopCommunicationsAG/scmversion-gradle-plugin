package com.intershop.gradle.scm.task

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option

/**
 * This is the base task for all tasks
 * with a dryrun option of this plugin.
 */
abstract class AbstractDryRunTask: AbstractReleaseVersionTask() {

    @get:Internal
    protected var dryRunProp: Boolean = false

    /**
     * If this property set to true. The task will do nothing and
     * show only the output. This is a commanline option.
     *
     * @property dryRun
     */
    @set:Option(option = "dryRun", description = "SCM version tasks run without any scm action.")
    @get:Input
    var dryRun: Boolean
        get() = dryRunProp
        set(value) {
            dryRunProp = value
        }
}
