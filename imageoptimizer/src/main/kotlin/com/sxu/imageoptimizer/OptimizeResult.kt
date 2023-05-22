package com.sxu.imageoptimizer

/*******************************************************************************
 * 优化结果
 *
 * @author: Freeman
 *
 * @date: 2023/4/29
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
data class OptimizeResult(
    /**
     * 优化前的大小
     */
    var beforeSize: Long = 0L,
    /**
     * 优化后的大小
     */
    var afterSize: Long = 0L,
    /**
     * 优化过的对象列表
     */
    var optimizedList: MutableList<ImageInfo> = mutableListOf()
)