package com.yin_radio.yin_radio_android_app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PageInfoDto(
    val page: Int,
    val path: String,
    val count: Int
)
