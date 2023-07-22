package com.jason.cloud.drive.model

import com.jason.cloud.extension.forEachObject
import org.json.JSONObject

data class FileListRespondEntity(
    val hash: String,
    val name: String,
    val path: String,
    val list: List<FileEntity>,
    val navigation: List<FileNavigationEntity>
) {
    companion object {
        fun createFromJson(obj: JSONObject): FileListRespondEntity {
            return FileListRespondEntity(
                obj.getString("hash"),
                obj.getString("name"),
                obj.getString("path"),
                ArrayList<FileEntity>().apply {
                    obj.getJSONArray("list").forEachObject { child ->
                        add(
                            FileEntity.createFromJson(child)
                        )
                    }
                },
                ArrayList<FileNavigationEntity>().apply {
                    obj.getJSONArray("navigation").forEachObject { child ->
                        add(
                            FileNavigationEntity(
                                child.getString("name"),
                                child.getString("hash")
                            )
                        )
                    }
                }
            )
        }
    }
}