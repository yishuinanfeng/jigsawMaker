package calculatorapp.free.quick.com.jigsawsample

import android.content.Context
import android.support.annotation.RawRes
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 创建时间： 2019/4/11
 * 作者：yanyinan
 * 功能描述：读raw下的文本文件
 */

fun readFile(context: Context, @RawRes resourceId: Int): String {
//    var reader: BufferedReader? = null
    var result = ""
//        val fileInputStream = FileInputStream(resourceId)
//        val inputStreamReader = InputStreamReader(fileInputStream, "UTF-8")
    var tempString: String
    val inputStream = context.resources.openRawResource(resourceId)
    val inputStreamReader = InputStreamReader(inputStream)
    BufferedReader(inputStreamReader).use {
        while (true) {
            tempString = it.readLine() ?: break
            result += tempString
        }
    }
    return result
}