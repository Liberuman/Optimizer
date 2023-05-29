package com.sxu.imageoptimizer

/*******************************************************************************
 * An enumeration representing the image compression formats that are supported by this system.
 * It includes support for all image formats, as well as specific support for JPEG, PNG,
 * and WEBP formats.
 *
 * @author: Freeman
 *
 * @date: 2023/4/26
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
enum class SupportFormatEnum(private val value: Int) {

    /**
     * Support compression for all image formats
     */
    SUPPORT_ALL(0),

    /**
     * Only support JPEG compression
     */
    SUPPORT_JPEG_ONLY(1),

    /**
     * Only support PNG compression
     */
    SUPPORT_PNG_ONLY(2),

    /**
     * Only support WEBP compression
     */
    SUPPORT_WEBP_ONLY(3),
}