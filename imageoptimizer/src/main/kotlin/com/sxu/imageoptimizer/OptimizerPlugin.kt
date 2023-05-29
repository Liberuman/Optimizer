package com.sxu.imageoptimizer

import org.gradle.api.Plugin
import org.gradle.api.Project

/*******************************************************************************
 * The implements of compress images
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
class OptimizerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.extensions.create("optimizerConfig", OptimizerConfig::class.java)
        project.afterEvaluate {
            project.tasks.create("ImageOptimizer", OptimizerTask::class.java)
        }
    }
}