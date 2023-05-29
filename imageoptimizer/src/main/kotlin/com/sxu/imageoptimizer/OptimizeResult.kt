package com.sxu.imageoptimizer

/*******************************************************************************
 * The result of compression images
 *
 * @author: Freeman
 *
 * @date: 2023/4/29
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
data class OptimizeResult(

    /**
     * The compression limit for the month is exceeded
     */
    var overLimit: Boolean = false,

    /**
     * Size before optimization
     */
    var beforeSize: Long = 0L,

    /**
     * Size after optimization
     */
    var afterSize: Long = 0L,

    /**
     * List of compressed ImageInfo objects
     */
    val optimizedList: MutableList<ImageInfo> = mutableListOf()
)