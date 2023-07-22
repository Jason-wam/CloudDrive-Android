package com.jason.cloud.extension

import android.accounts.NetworkErrorException
import android.content.ActivityNotFoundException
import org.json.JSONException
import java.io.EOFException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.BindException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.EmptyStackException

fun Throwable?.toFullMessage(): String {
    return this?.stackTraceToString()?.replace("(http|https)://.*?/".toRegex(), "http://*.*.*.*/")
        ?: "未知错误"
}

fun Throwable?.toMessage(): String {
    this?.printStackTrace()
    return when (this) {
        is UnknownHostException -> "未知主机异常"
        is JSONException -> "JSON异常"
        is SocketTimeoutException -> "套接字超时"
        is FileNotFoundException -> "文件不存在"
        is ConnectException -> "连接异常"

        is ActivityNotFoundException -> "未找到Activity异常"
        is ArrayStoreException -> "阵列存储异常"
        is CloneNotSupportedException -> "不支持克隆异常"
        is IllegalAccessException -> "非法访问异常"
        is NumberFormatException -> "数字转换异常"
        is EnumConstantNotPresentException -> "枚举常量不存在异常"
        is IllegalMonitorStateException -> "非法监视器状态异常"
        is IllegalThreadStateException -> "非法线程状态异常"
        is InstantiationException -> "实例化异常"
        is InterruptedException -> "线程中断异常"
        is NegativeArraySizeException -> "数组负长度异常"
        is NoSuchFieldException -> "无此类字段例外"
        is NoSuchMethodException -> "无此类方法异常"
        is AccessDeniedException -> "拒绝访问异常"
        is TypeNotPresentException -> "类型不存在异常"
        is ConcurrentModificationException -> "并发修改异常"
        is FileAlreadyExistsException -> "文件已存在异常"
        is NoSuchFileException -> "意外达到文件末尾异常"
        is ArithmeticException -> "算术运算异常"
        is CharacterCodingException -> "字符编码异常"
        is IllegalArgumentException -> "非法参数异常"
        is KotlinNullPointerException -> "Kotlin空指针异常"
        is NoWhenBranchMatchedException -> "分支匹配异常"
        is IllegalStateException -> "非法状态异常"
        is NoSuchElementException -> "无此类元素异常"
        is TypeCastException -> "类型强制转换异常"
        is ArrayIndexOutOfBoundsException -> "数组下标越界"
        is StringIndexOutOfBoundsException -> "字符串下标越界"
        is NullPointerException -> "空指针访问异常"
        is ClassCastException -> "类型强制转换异常"
        is EmptyStackException -> "空堆栈异常"
        is NetworkErrorException -> "网络错误"
        is FileSystemException -> "文件系统异常"
        is EOFException -> "意外达到文件末尾异常"
        is BindException -> message.toString()
        is IOException -> "输入输出错误"
        is ClassNotFoundException -> "未找到类异常"
        is ReflectiveOperationException -> "反射操作异常"
        is UninitializedPropertyAccessException -> "未初始化的属性访问异常"
        is SecurityException -> "安全异常"
        is RuntimeException -> "运行时异常"
        else -> "错误：${this?.message?.replace("(http|https)://.*?/".toRegex(), "http://*.*.*.*/")}"
    }
}