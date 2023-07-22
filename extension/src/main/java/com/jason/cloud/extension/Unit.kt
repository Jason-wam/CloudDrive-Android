package com.jason.cloud.extension

inline val Double.KB: Long
    get() = run {
        return (this * 1024).toLong()
    }

inline val Double.MB: Long
    get() = run {
        return (this * 1024 * 1024).toLong()
    }

inline val Double.GB: Long
    get() = run {
        return (this * 1024 * 1024 * 1024).toLong()
    }

inline val Int.KB: Long
    get() = run {
        return (this * 1024).toLong()
    }

inline val Int.MB: Long
    get() = run {
        return (this * 1024 * 1024).toLong()
    }

inline val Int.GB: Long
    get() = run {
        return (this.toFloat() * 1024 * 1024 * 1024).toLong()
    }

inline val Float.KB: Long
    get() = run {
        return (this * 1024).toLong()
    }

inline val Float.MB: Long
    get() = run {
        return (this * 1024 * 1024).toLong()
    }

inline val Float.GB: Long
    get() = run {
        return (this * 1024 * 1024 * 1024).toLong()
    }
