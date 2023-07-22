package com.jason.videoview.model

import android.graphics.Color
import java.io.Serializable

class DanmakuEntity : Serializable {
    var type: Int = 1
    var size: Int = 13
    var time: Long = 0
    var text: String = ""
    var color: Int = Color.WHITE
}