package com.sxu.imageoptimizer

/*******************************************************************************
 * 支持压缩的图片格式
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
enum class SupportFormatEnum(private val value: Int) {

    /**
     * 支持压缩所有图片格式
     */
    SUPPORT_ALL(0),

    /**
     * 只支持JPEG格式压缩
     */
    SUPPORT_JPEG_ONLY(1),

    /**
     * 只支持PNG格式压缩
     */
    SUPPORT_PNG_ONLY(2),

    /**
     * 只支持WEBP格式压缩
     */
    SUPPORT_WEBP_ONLY(3),
}