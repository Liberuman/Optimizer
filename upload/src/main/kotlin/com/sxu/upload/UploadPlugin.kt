package com.sxu.upload

import org.gradle.api.Plugin
import org.gradle.api.Project

/*******************************************************************************
 * Apk文件上传插件的实现
 *
 * @author: Freeman
 *
 * @date: 2023/5/19
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
class UploadPlugin: Plugin<Project>{

    override fun apply(project: Project) {
        project.extensions.create("uploadConfig", UploadConfig::class.java)
        project.afterEvaluate {
            project.tasks.create("Upload", UploadTask::class.java)
        }
    }
}