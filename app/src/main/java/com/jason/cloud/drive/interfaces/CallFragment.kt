package com.jason.cloud.drive.interfaces

interface CallFragment {
    fun callBackPressed(): Boolean {
        return true
    }

    /**
     * activity调用Fragment
     */
    fun locateFileLocation(hash: String, fileHash: String) {

    }
}