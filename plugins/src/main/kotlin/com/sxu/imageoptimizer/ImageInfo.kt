package com.sxu.imageoptimizer

/*******************************************************************************
 * 图片的基本信息
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
data class ImageInfo(
    /**
     * 图片路径
     */
    val path : String = "",

    /**
     * 压缩前的图片大小, 单位字节
     */
    val beforeSize: Long = 0,

    /**
     * 压缩后的图片大小, 单位字节
     */
    val afterSize : Long = 0,

    /**
     * 图片路径的Md5值
     */
    val md5: String = "",

    /**
     * 该图片是否需要忽略，当大小小于配置的skipSize时不压缩
     */
    val ignore: Boolean = false,
)
