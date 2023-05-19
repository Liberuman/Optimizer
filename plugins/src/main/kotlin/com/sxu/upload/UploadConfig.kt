package com.sxu.upload

/*******************************************************************************
 * 上传配置类
 *
 * @author: Freeman
 *
 * @date: 2023/5/19
 *
 * Copyright: all rights reserved by Freeman.
 *******************************************************************************/
open class UploadConfig(

    /**
     * Apk的路径
     */
    val apkPath: String = "",

    /**
     * 目标网站的key
     */
    val apiKey: String = "",

    /**
     * 目标网站的地址
     */
    val url: String = "",

    /**
     * 是否自动通知
     */
    val autoNotify: Boolean = false,

    /**
     * 通知的内容
     */
    val notifyContent: String = "",
)
