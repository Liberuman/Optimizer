package com.sxu.upload

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/*******************************************************************************
 * 图片压缩实现过程
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
open class UploadTask : DefaultTask() {

    init {
        group = "optimizer"
    }

    @TaskAction
    fun run() {

    }

    private fun log(msg: String) {
        println("Optimizer: $msg")
    }
}