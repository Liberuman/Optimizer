package com.sxu.imageoptimizer

/*******************************************************************************
 * The basic information of the compressed image.
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
data class ImageInfo(
    /**
     * The path of the image.
     */
    val path: String = "",

    /**
     *  The size of the image before compression, in bytes.
     */
    val beforeSize: Long = 0,

    /**
     * The size of the image after compression, in bytes.
     */
    val afterSize: Long = 0,

    /**
     * The MD5 hash value of the image path.
     */
    val md5: String = "",

    /**
     * Whether to ignore this image. When the size is smaller than the skipSize configured, it will not be compressed.
     */
    val ignore: Boolean = false,
)
