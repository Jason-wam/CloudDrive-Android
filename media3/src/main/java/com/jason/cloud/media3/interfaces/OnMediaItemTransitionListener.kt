package com.jason.cloud.media3.interfaces

import com.jason.cloud.media3.model.Media3Item

interface OnMediaItemTransitionListener {
    fun onTransition(index: Int, item: Media3Item)
}