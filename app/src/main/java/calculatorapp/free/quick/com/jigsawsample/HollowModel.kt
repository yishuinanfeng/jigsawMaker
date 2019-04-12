package calculatorapp.free.quick.com.jigsawsample

import android.graphics.Path
import android.support.annotation.IntDef

/**
 * 创建时间： 2019/4/2
 * 作者：yanyinan
 * 功能描述：
 * @param path:用于canvas进行clip的路径
 */
data class HollowModel(var hollowX: Int,var hollowY: Int,var initWidth: Int,var initHeight: Int,val path:Path? = null) {
    var width:Int = initWidth
    var height:Int = initHeight

    companion object {
        /**
         * 表示对应的边（没有、上、左、下、右）
         */
        const val NO_SIDE = -1
        const val LEFT = 0
        const val TOP = 1
        const val RIGHT = 2
        const val BOTTOM = 3
    }

    var selectSide:Int = NO_SIDE
}