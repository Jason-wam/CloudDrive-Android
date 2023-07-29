package com.jason.cloud.drive.model

import com.jason.cloud.extension.forEachObject
import org.json.JSONObject

data class SearchRespondEntity(
    val page: Int,
    val count: Int,
    val hasMore: Boolean,
    val list: List<FileEntity>
) {
    companion object {
        fun createFromJson(obj: JSONObject): SearchRespondEntity {
            return SearchRespondEntity(
                obj.getInt("page"),
                obj.getInt("count"),
                obj.getBoolean("hasMore"),
                ArrayList<FileEntity>().apply {
                    obj.getJSONArray("list").forEachObject { child ->
                        add(
                            FileEntity.createFromJson(child)
                        )
                    }
                }
            )
        }
    }
}