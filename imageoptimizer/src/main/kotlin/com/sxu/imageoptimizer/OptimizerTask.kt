package com.sxu.imageoptimizer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.tinify.AccountException
import com.tinify.Options
import com.tinify.Tinify
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.com.google.common.net.MediaType.*
import java.io.File
import java.io.InputStreamReader
import java.math.BigInteger
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.text.DecimalFormat

/*******************************************************************************
 * The implementation process of image compression.
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
open class OptimizerTask : DefaultTask() {

    /**
     * The image paths to be optimized
     */
    private val allImagePathList = mutableSetOf<String>()

    init {
        group = "optimizer"
    }

    @TaskAction
    fun run() {
        // Get the optimizer configuration
        val config = project.extensions.getByName("optimizerConfig") as? OptimizerConfig?
        if (config == null) {
            log("Please configure the ImageOptimizer task")
            return
        }
        // Check API key
        if (config.apiKey.isNullOrEmpty()) {
            log("API Key not configured")
            return
        }
        // Validate configuration
        if (!config.isAppendMode && config.resourceDirs.isNullOrEmpty()) {
            log("Configuration error: resourceDirs cannot be empty when isAppendMode is false")
            return
        }
        // Check if the API key is valid
        if (!checkApiKey(config.apiKey)) {
            log("Invalid TinyPng API Key")
            return
        }
        // Retrieve the previously optimized image information
        val optimizedListFile = File("${project.rootDir}/compressedList.json")
        val optimizedList = if (optimizedListFile.exists()) {
            try {
                val reader = InputStreamReader(optimizedListFile.inputStream())
                Gson().fromJson<List<ImageInfo>>(
                    reader,
                    object : TypeToken<List<ImageInfo>>() {}.type
                ) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace(System.err)
                emptyList()
            }
        } else {
            emptyList()
        }
        // Retrieve the list of directories to be optimized
        val waitOptimizerDirs = mutableListOf<String>()
        if (config.isAppendMode) {
            findAllImageDirs(config.supportFormat)
            waitOptimizerDirs.addAll(allImagePathList.toList())
            waitOptimizerDirs.addAll(config.resourceDirs)
        } else {
            waitOptimizerDirs.addAll(config.resourceDirs)
        }
        log(
            "Directories waiting for optimization: ${
                GsonBuilder().setPrettyPrinting().create().toJson(waitOptimizerDirs)
            }"
        )
        // Convert to WebP format if requested
        if (config.convertToWebP) {
            convertToWebp(waitOptimizerDirs, config.whiteList, config.supportFormat)
            if (config.onlyConvert) {
                log("All images have been converted to WebP...")
                return
            }
        }
        // Calculate the total size of images before and after optimization
        var beforeSize = 0L
        var afterSize = 0L
        log("Start optimization...")
        var overLimit = false
        val newOptimizedList = mutableListOf<ImageInfo>()
        for (dir in waitOptimizerDirs) {
            val file = File(dir)
            if (!file.exists() || !file.isDirectory) {
                continue
            }
            val optimizeResult = startOptimize(file, config, optimizedList)
            // If the free usage limit has been exceeded, exit the loop
            if (optimizeResult.overLimit) {
                overLimit = true
                break
            }
            // If no images meet the criteria, move on to the next directory
            if (optimizeResult.optimizedList.isNullOrEmpty()) {
                continue
            }
            beforeSize += optimizeResult.beforeSize
            afterSize += optimizeResult.afterSize
            newOptimizedList.addAll(optimizeResult.optimizedList)
        }
        if (newOptimizedList.isEmpty()) {
            if (!overLimit) {
                log("No images need to be compressed!!!")
            }
            return
        }
        // Add the newly optimized images to the list of optimized images
        val finalOptimizedList = optimizedList + newOptimizedList
        if (!optimizedListFile.exists()) {
            optimizedListFile.createNewFile()
        }
        optimizedListFile.writeText(
            GsonBuilder().setPrettyPrinting().create().toJson(finalOptimizedList),
            Charset.forName("utf-8")
        )
        log(
            "Task finished! Optimized ${newOptimizedList.size} images, before total size: ${
                beforeSize.formatSize()
            }, after total size: ${afterSize.formatSize()}, decreased ${(beforeSize - afterSize).formatSize()}"
        )
    }

    /**
     * Check if the TinyPng API Key is valid
     * @param apiKey: TinyPng API Key
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
     * Find all image directories
     * @param format: the image file format of found
     */
    private fun findAllImageDirs(format: SupportFormatEnum) {
        project.rootProject.allprojects.forEach {
            // Find image paths in each module's 'assets' folder
            File("${project.rootDir.path}/${it.name}/src/main/assets").apply {
                if (exists()) {
                    findImagePath(this, format)
                }
            }
            // Find image paths in each module's 'src' folder
            File("${project.rootDir.path}/${it.name}/src/main/res").apply {
                if (exists()) {
                    findImagePath(this, format)
                }
            }
        }
    }

    /** Finds directories containing image files under the specified path.
     * @param directory: directory to search in.
     * @param format: the image file format of found
     */
    private fun findImagePath(directory: File, format: SupportFormatEnum) {
        if (!directory.isDirectory) {
            return
        }
        directory.listFiles()?.apply {
            // Search in subdirectories.
            filter { it.isDirectory }.forEach { subDirectory ->
                findImagePath(subDirectory, format)
            }
            // Check if current directory contains specific format image files.
            val imageFile = filter { !it.isDirectory }.find { it.matchedFileFormat(format) }
            imageFile?.parent?.let { allImagePathList.add(it) }
        }
    }

    /* Converts images in specified directories to WebP format
     * @param targetDirectories: Directories to convert images from
     * @param whitelist: Optional list of filenames to exclude from conversion
     * @param format: the image file format of waiting convert
     */
    private fun convertToWebp(
        targetDirectories: List<String>,
        whitelist: List<String>?,
        format: SupportFormatEnum
    ) {
        for (directory in targetDirectories) {
            val file = File(directory)
            if (!file.exists() || !file.isDirectory) {
                continue
            }
            file.listFiles().asSequence()
                .filter {
                    it.matchedFileFormat(format) &&
                            !it.name.toLowerCase().endsWith(".webp") &&
                            whitelist?.contains(it.name) != true
                }
                .forEach {
                    try {
                        val result = Tinify.fromFile(it.path)
                            .convert(Options().with("type", arrayOf("image/webp")))
                            .result()
                        result.toFile("${it.parent}/${it.nameWithoutExtension}.${result.extension()}")
                    } catch (e: Exception) {
                        log(e.message ?: "An exception occurred")
                    }
                }
        }
    }

    /**
     * Optimizes images in the target directory using the given configuration and list of already
     * optimized images.Only images that are not in the white list, not already optimized, and
     * larger than the skip size can be optimized.
     * @param targetDirectory the directory containing the images to optimize
     * @param config the configuration options to use for optimization
     * @param optimizedList the list of already optimized images
     * @return an OptimizeResult object containing information about the optimization process
     */
    private fun startOptimize(
        targetDirectory: File,
        config: OptimizerConfig,
        optimizedList: List<ImageInfo>?
    ): OptimizeResult {
        val result = OptimizeResult()
        // Only process image files that meet the conditions for optimization
        targetDirectory.listFiles()
            .asSequence()
            .filter {
                it.matchedFileFormat(config.supportFormat)
                        && it.length() > config.skipSize
                        && config.whiteList?.contains(it.name) != true
                        && optimizedList?.find { it1 -> it1.path == it.path }.run {
                    this == null || md5 != it.getFileMd5()
                }
            }.apply {
                for (file in this) {
                    val beforeSize = file.length()
                    try {
                        // Optimize the image using the Tinify API
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
                        // Write the optimized image to disk
                        optimizedFile.toFile("${file.path}")
                        // Add information about the optimized image to the result object
                        val imageInfo = ImageInfo(
                            path = file.path,
                            beforeSize = beforeSize,
                            afterSize = optimizedFile.size().toLong(),
                            md5 = file.getFileMd5()
                        )
                        result.optimizedList.add(imageInfo)
                        result.beforeSize += imageInfo.beforeSize
                        result.afterSize += optimizedFile.size()
                    } catch (e: AccountException) {
                        // Handle errors related to the Tinify API account
                        result.overLimit = true
                        log(e.message ?: "An exception occurred")
                        break
                    } catch (e: Exception) {
                        // Handle other types of exceptions
                        log(e.message ?: "An exception occurred")
                        break
                    }
                }
            }

        return result
    }

    /**
     * Calculates the MD5 hash of a file.
     */
    private fun File.getFileMd5(): String {
        // Generate unique key using file path and last modified time
        val key = "${path}${Files.getLastModifiedTime(Paths.get(path)).toMillis()}"
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(key.toByteArray(Charset.forName("utf-8")))
        // Convert byte array to hexadecimal string and pad with zeroes if necessary
        return BigInteger(1, bytes).toString(16).padStart(32, '0')
    }

    /**
     * Checks whether the file matches the given format.
     * @return true if this file type matched; false otherwise.
     */
    private val jpgExtension = "jpg"
    private fun File.matchedFileFormat(format: SupportFormatEnum): Boolean {
        val extension = this.extension
        return when (format) {
            SupportFormatEnum.SUPPORT_WEBP_ONLY -> extension.equals(WEBP.subtype(), true)
            SupportFormatEnum.SUPPORT_PNG_ONLY -> extension.equals(PNG.subtype(), true)
            SupportFormatEnum.SUPPORT_JPEG_ONLY -> extension.equals(
                JPEG.subtype(),
                true
            ) || extension.equals(jpgExtension, true)
            else -> {
                when (extension) {
                    WEBP.subtype(), JPEG.subtype(), jpgExtension -> true
                    PNG.subtype() -> !this.nameWithoutExtension.contains(".9.")
                    else -> false
                }
            }
        }
    }

    /**
     * Formats file size into a user-readable string
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