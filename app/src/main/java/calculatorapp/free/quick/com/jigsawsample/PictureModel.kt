package calculatorapp.free.quick.com.jigsawsample

import android.graphics.Bitmap

/**
 * 创建时间： 2019/4/2
 * 作者：yanyinan
 * 功能描述：代表一张图片的模型。xToHollowCenter，y为图片相对边框中点的偏移
 */
data class PictureModel(val bitmapPicture:Bitmap, val hollowModel: HollowModel, var xToHollowCenter:Int = 0, var yToHollowCenter:Int = 0) {
    val initScale:Float
    var scale:Float

    init {
        val hollowWidth = hollowModel.width
        val hollowHeight = hollowModel.height
        initScale = getCenterPicScale(bitmapPicture,hollowWidth,hollowHeight)
        scale = initScale
    }


    /**
     * 得到在固定的显示尺寸限定得Bitmap显示centerCrop效果的缩放比例
     */
    private fun getCenterPicScale(bitmap: Bitmap, width: Int, height: Int): Float {
        val widthBmp = bitmap.width
        val heightBmp = bitmap.height
        val scale: Float
        scale = if (widthBmp < heightBmp) {
            width / widthBmp.toFloat()
        } else {
            height / heightBmp.toFloat()
        }

        return scale
    }
}