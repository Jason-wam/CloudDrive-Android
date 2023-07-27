package com.jason.cloud.drive.interfaces

interface CallMainActivity {
    /**
     * 用于Fragment调用Activity方法的接口
     */
    fun locateFileLocation(hash: String, fileHash: String) {

    }
}