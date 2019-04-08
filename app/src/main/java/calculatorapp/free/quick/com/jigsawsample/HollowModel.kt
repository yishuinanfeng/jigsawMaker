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
        /**
         * 表示对应的边（没有、上、左、下、右）
         */
        const val NO_SIDE = 0
        const val LEFT = 1
        const val TOP = 2
        const val RIGHT = 3
        const val BOTTOM = 4
    }

    var selectSide:Int = NO_SIDE
}