package com.sxu.imageoptimizer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.tinify.*
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
import kotlin.Exception

/*******************************************************************************
 * 图片压缩实现过程
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
open class OptimizerTask : DefaultTask() {

    /**
     * 所有图片的路径
     */
    private val allImagePathList = mutableSetOf<String>()

    init {
        group = "optimizer"
    }

    @TaskAction
    fun run() {
        val config = project.extensions.getByName("optimizerConfig") as? OptimizerConfig?
        if (config == null) {
            log("Please config ImageOptimizer task")
            return
        }

        if (config.apiKey.isNullOrEmpty()) {
            log("Not config apiKey")
            return
        }

        if (!config.isAppendMode && config.resourceDirs.isNullOrEmpty()) {
            log("Config error: resourceDirs can't be empty when isAppendMode is false")
            return
        }

        if (!checkApiKey(config.apiKey)) {
            log("TinyPng apiKey invalid")
            return
        }

        // 获取已优化的图片信息
        var optimizedList = mutableListOf<ImageInfo>()
        val optimizedListFile = File("${project.rootDir}/compressedList.json")
        if (optimizedListFile.exists()) {
            try {
                val reader = InputStreamReader(optimizedListFile.inputStream())
                var tempList = Gson().fromJson<List<ImageInfo>>(
                    reader,
                    object : TypeToken<List<ImageInfo>>() {}.type
                )
                if (!tempList.isNullOrEmpty()) {
                    optimizedList.addAll(tempList)
                }
            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }
        }

        // 根据配置获取待优化的路径
        val waitOptimizerDirs = mutableListOf<String>()
        if (config.isAppendMode) {
            findAllImageDirs()
            waitOptimizerDirs.addAll(allImagePathList.toList())
            waitOptimizerDirs.addAll(config.resourceDirs)
        } else {
            waitOptimizerDirs.addAll(config.resourceDirs)
        }
        log(
            "Waiting optimized directory: ${
                GsonBuilder().setPrettyPrinting().create().toJson(waitOptimizerDirs)
            }"
        )

        if (config.convertToWebP) {
            convertToWebP(waitOptimizerDirs, config.whiteList)
            if (config.onlyConvert) {
                log("All pictures have converted to webP...")
                return
            }
        }

        var beforeSize = 0L
        var afterSize = 0L
        log("Start Optimize...")
        var overLimit = false
        val newOptimizedList = mutableListOf<ImageInfo>()
        for (dir in waitOptimizerDirs) {
            val file = File(dir)
            if (!file.exists() || !file.isDirectory) {
                continue
            }

            val optimizeResult = startOptimize(file, config, optimizedList)
            // 如果免费次数已用完, 直接退出
            if (optimizeResult.overLimit) {
                overLimit = true
                break
            }
            // 如果没有找到满足条件的, 则压缩下一个目录
            if (optimizeResult.optimizedList.isNullOrEmpty()) {
                continue
            }

            beforeSize += optimizeResult.beforeSize
            afterSize += optimizeResult.afterSize
            newOptimizedList.addAll(optimizeResult.optimizedList)
        }

        if (newOptimizedList.isEmpty()) {
            if (!overLimit) {
                log("No picture need compress!!!")
            }
            return
        }

        optimizedList.addAll(newOptimizedList)
        if (!optimizedListFile.exists()) {
            optimizedListFile.createNewFile()
        }
        optimizedListFile.writeText(Gson().toJson(optimizedList), Charset.forName("utf-8"))
        log(
            "Task finished! optimized ${newOptimizedList.size} files, before total size: ${
                beforeSize.formatSize()
            }, after total size: ${afterSize.formatSize()}, decreased ${(afterSize - beforeSize).formatSize()}"
        )
    }

    /**
     * 检查TinyPng Api Key是否可用
     * @param apiKey: TinyPng Api Key
     */
    private fun checkApiKey(apiKey: String): Boolean {
        try {
            Tinify.setKey("$apiKey")
            Tinify.validate()
            return true
        } catch (e: Exception) {
            log(e.message ?: "")
        }

        return false
    }

    private fun log(msg: String) {
        println("Optimizer: $msg")
    }

    /**
     * 查找所有图片的路径
     */
    private fun findAllImageDirs() {
        project.rootProject.allprojects.forEach {
            // 在每个模块的assets文件夹中查找包含图片的路径
            File("${project.rootDir.path}/${it.name}/src/main/assets").apply {
                if (exists()) {
                    findImagePath(this)
                }
            }

            // 在每个模块的src文件夹中查找包含图片的路径
            File("${project.rootDir.path}/${it.name}/src/main/res").apply {
                if (exists()) {
                    findImagePath(this)
                }
            }
        }
    }

    /**
     * 在指定路径下查找包含图片的文件夹
     * @param directoryFile: 要查找的目录
     */
    private fun findImagePath(directoryFile: File) {
        if (!directoryFile.isDirectory) {
            return
        }

        directoryFile.listFiles().apply {
            // 在子目录中查找
            filter { it.isDirectory }.forEach {
                findImagePath(it)
            }

            // 判断当前路径是否包含图片
            val allFiles = filter { !it.isDirectory }
            for (file in allFiles) {
                if (file.isImageFile()) {
                    allImagePathList.add(file.parent)
                    break
                }
            }
        }
    }

    /**
     * 将指定路径的图片转换为WebP格式
     * @param targetDirs: 要转换的图片目录
     */
    private fun convertToWebP(targetDirs: List<String>, whiteList: List<String>?) {
        for (dir in targetDirs) {
            val file = File(dir)
            if (!file.exists() || !file.isDirectory) {
                continue
            }

            file.listFiles().asSequence().filter {
                it.isImageFile()
                        && !it.name.toLowerCase().endsWith(".webp")
                        && whiteList?.contains(it.name) != true
            }.forEach {
                try {
                    val result = Tinify.fromFile(it.path).convert(
                        Options()
                            .with("type", arrayOf("image/webp"))
                    )
                        .result()
                    result.toFile("${it.parent}/${it.nameWithoutExtension}.${result.extension()}")
                } catch (e: Exception) {
                    log(e.message ?: "Occur a exception")
                }
            }
        }
    }

    /**
     * 开始优化
     * @param targetDirectory: 待优化的目录
     * @param config: 优化配置
     * @param optimizedList: 已优化的列表
     */
    private fun startOptimize(
        targetDirectory: File,
        config: OptimizerConfig,
        optimizedList: List<ImageInfo>?
    ): OptimizeResult {
        val result = OptimizeResult()
        // 只有不包含在白名单和已优化列表中, 且图片大小大于skipSize的图片才可进行优化
        targetDirectory.listFiles().asSequence().filter {
            it.isImageFile()
                    && it.length() > config.skipSize
                    && config.whiteList?.contains(it.name) != true
                    && optimizedList?.find { it1 -> it1.path == it.path }.run {
                this == null || md5 != it.getFileMd5()
            }
        }.apply {
            for (file in this) {
                val beforeSize = file.length()
                try {
                    val optimizedFile = Tinify.fromFile(file.path).result()
                    log("Find target picture -> ${file.path}")
                    if (beforeSize > 0) {
                        val compressRatio =
                            (beforeSize - optimizedFile.size()) * 100.0f / beforeSize
                        // 图片压缩率低于指定阈值时，忽略压缩结果
                        if (compressRatio >= 0 && compressRatio < config.compressRatioThreshold) {
                            continue
                        }
                    }

                    optimizedFile.toFile("${file.path}")

                    val imageInfo = ImageInfo(
                        path = file.path,
                        beforeSize = beforeSize,
                        afterSize = optimizedFile.size().toLong(),
                        md5 = file.getFileMd5()
                    )
                    result.optimizedList.add(imageInfo)
                    result.beforeSize += imageInfo.beforeSize
                    result.afterSize += optimizedFile.size()
                } catch(e: AccountException) {
                    result.overLimit = true
                    log(e.message ?: "Occur a exception")
                    break
                } catch (e: Exception) {
                    log(e.message ?: "Occur a exception")
                    break
                }
            }
        }

        return result
    }

    /**
     * 获取文件的Md5值
     */
    private fun File.getFileMd5(): String {
        val key = "${path}${Files.getLastModifiedTime(Paths.get(path)).toMillis()}"
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(key.toByteArray(Charset.forName("utf-8")))
        return BigInteger(1, bytes).toString(16).padStart(32, '0')
    }

    /**
     * 该文件是否为图片资源
     * @return true表示该文件为图片，否则不是
     */
    private fun File.isImageFile(): Boolean {
        return path.toLowerCase().run {
            endsWith(".webp") || (endsWith(".png") && !contains(".9.")) || endsWith(".jpg") || endsWith(
                ".jpeg"
            )
        }
    }

    /**
     * 格式化文件大小
     */
    private fun Long.formatSize(): String {
        val df = DecimalFormat("#.00")
        if (this == 0L) {
            return "0B"
        }

        return if (this < 1024) {
            df.format(this) + "B"
        } else if (this < 1024 * 1024) {
            df.format(this.toDouble() / 1024) + "KB"
        } else if (this < 1024 * 1024 * 1024) {
            df.format(this.toDouble() / (1024 * 1024)) + "MB"
        } else {
            df.format(this.toDouble() / 1024 * 1024 * 1024) + "GB"
        }
    }

}