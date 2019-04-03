package calculatorapp.free.quick.com.jigsawsample

import android.graphics.Path
import android.support.annotation.IntDef

/**
 * 创建时间： 2019/4/2
 * 作者：yanyinan
 * 功能描述：
 */
data class HollowModel(var hollowX: Int,var hollowY: Int,var initWidth: Int,var initHeight: Int,val path:Path? = null) {
    var width:Int = initWidth
    var height:Int = initHeight

    companion object {
        val NO_SIDE = 0
        val LEFT = 1
        val TOP = 2
        val RIGHT = 3
        val BOTTOM = 4
    }

    var selectSide:Int = NO_SIDE
}