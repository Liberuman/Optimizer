package com.sxu.upload

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tinify.Tinify
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.InputStreamReader
import java.math.BigInteger
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.text.DecimalFormat

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