package com.jason.cloud.drive.utils.extension

import android.accounts.NetworkErrorException
import android.content.ActivityNotFoundException
import com.drake.net.exception.ConvertException
import com.drake.net.exception.DownloadFileException
import com.drake.net.exception.HttpFailureException
import com.drake.net.exception.NetConnectException
import com.drake.net.exception.NetException
import com.drake.net.exception.NetSocketTimeoutException
import com.drake.net.exception.NetworkingException
import com.drake.net.exception.NoCacheException
import com.drake.net.exception.RequestParamsException
import com.drake.net.exception.ResponseException
import com.drake.net.exception.ServerResponseException
import com.drake.net.exception.URLParseException
import okio.EOFException
import okio.FileNotFoundException
import okio.IOException
import org.json.JSONException
import java.net.BindException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.EmptyStackException

fun Throwable?.toFullMessage(): String {
    return this?.stackTraceToString()?.replace("(http|https)://.*?/".toRegex(), "http://*.*.*.*/") ?: "未知错误"
}

fun Throwable?.toMessage(): String {
    this?.printStackTrace()
    return when (this) {
        is UnknownHostException -> "未知主机异常"
        is URLParseException -> "URL解析异常"
        is NetConnectException -> "网络连接异常"
        is NetworkingException -> "网络错误"
        is NetSocketTimeoutException -> "连接服务器超时"
        is DownloadFileException -> "文件下载异常"
        is ConvertException -> "转换异常"
        is RequestParamsException -> "客户端请求异常，错误原因：${response.code}"
        is ServerResponseException -> "网络错误，服务器响应异常"
        is NoCacheException -> "网络请求失败，且未读取到缓存文件"
        is ResponseException -> "网络错误，返回数据异常"
        is HttpFailureException -> "网络请求失败"
        is NetException -> "网络错误"
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