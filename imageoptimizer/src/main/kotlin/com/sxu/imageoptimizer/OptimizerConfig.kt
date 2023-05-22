package com.sxu.imageoptimizer

/*******************************************************************************
 * 压缩配置类
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
open class OptimizerConfig(
    /**
     * TinyPng API Key
     */
    var apiKey: String = "",

    /**
     * 需要忽略压缩的图片大小，单位字节。比如10K以下的不需要压缩，直接配置为10 * 1024
     */
    var skipSize: Int = 0,

    /**
     * 支持压缩的图片格式
     */
    var supportFormat: SupportFormatEnum = SupportFormatEnum.SUPPORT_ALL,

    /**
     * 压缩比例阈值，只有压缩率高于此值时才会替换原始文件，推荐取值为30%
     */
    var compressRatioThreshold: Int = 30,

    /**
     * 是否为追加模式，默认为true，表示将resourceDirs路径添加到优化列表中
     * 否则只优化resourceDirs路径中的图片资源
     */
    var isAppendMode: Boolean = true,

    /**
     * 白名单，用于配置不需要压缩的资源文件或文件夹
     */
    var whiteList: List<String>? = listOf(),

    /**
     * 指定需要压缩的资源路径，默认会压缩项目中的所有图片，指定后则只压缩指定路径的图片
     */
    var resourceDirs: List<String> = listOf(),
)