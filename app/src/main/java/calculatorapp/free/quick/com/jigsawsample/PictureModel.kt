package calculatorapp.free.quick.com.jigsawsample

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent

/**
 * 创建时间： 2019/4/2
 * 作者：yanyinan
 * 功能描述：代表一张图片的模型。xToHollowCenter，y为图片相对边框中点的偏移
 */
data class PictureModel(val bitmapPicture: Bitmap, val hollowModel: HollowModel, var xToHollowCenter: Int = 0, var yToHollowCenter: Int = 0) {
    companion object {
        private const val HOLLOW_TOUCH_WIDTH = 100
        private const val HOLLOW_SCALE_UPPER_LIMIT = 1.5
        private const val HOLLOW_TOUCH_LOWER_LIMIT = 0.5
        private const val PICTURE_ANIMATION_DELAY = 100L
    }

    private val initScale: Float
    var scale: Float
    /**
     * 表示会被当前的PictureModel拖动影响到的其他PictureModel
     *
     * 外层Map的key：当前PictureModel所能拖动的边框。使用HollowModel的Left/Top/Right/bottom表示
     * 内层Map:Value为被当前PictureModel所联动的PictureModel，Key为被当前PictureModel所联动的PictureModel需要移动的边框（即拖动当前的PictureModel会影响到的边）
     * ，使用HollowModel的Left/Top/Right/bottom表示
     */
    val mEffectPictureModel: SparseArray<SparseArray<PictureModel>> = SparseArray()

    var isSelected: Boolean = false
    /**
     * 是否触摸到边框
     */
    var isTouchHollow = false

    init {
        val hollowWidth = hollowModel.width
        val hollowHeight = hollowModel.height
        initScale = getCenterPicScale(bitmapPicture, hollowWidth, hollowHeight)
        scale = initScale
    }

    private fun setScaleWithCondition(value: Float) {
        if (value < initScale) {
            scale = initScale
            return
        }
        scale = value
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

    /**
     * 刷新是否触摸到边框状态（即边框可拖动）
     */
    fun refreshIsTouchHollowState(event: MotionEvent) {

        val x = event.x.toInt()
        val y = event.y.toInt()

        val hollowX = hollowModel.hollowX
        val hollowY = hollowModel.hollowY
        val hollowWidth = hollowModel.width
        val hollowHeight = hollowModel.height

        val rectLeft = Rect(hollowX, hollowY, hollowX + HOLLOW_TOUCH_WIDTH, hollowY + hollowHeight)
        val rectTop = Rect(hollowX, hollowY, hollowX + hollowWidth, hollowY + HOLLOW_TOUCH_WIDTH)
        val rectRight = Rect(hollowX + hollowWidth - HOLLOW_TOUCH_WIDTH, hollowY, hollowX + hollowWidth, hollowY + hollowHeight)
        val rectBottom = Rect(hollowX, hollowY + hollowHeight - HOLLOW_TOUCH_WIDTH, hollowX + hollowWidth, hollowY + hollowHeight)

        //点在矩形区域中
        if (rectLeft.contains(x, y)) {
            hollowModel.selectSide = HollowModel.LEFT
            isTouchHollow = true
        }
        if (rectTop.contains(x, y)) {
            hollowModel.selectSide = HollowModel.TOP
            isTouchHollow = true
        }
        if (rectRight.contains(x, y)) {
            hollowModel.selectSide = HollowModel.RIGHT
            isTouchHollow = true
        }
        if (rectBottom.contains(x, y)) {
            hollowModel.selectSide = HollowModel.BOTTOM
            isTouchHollow = true
        }

//        isTouchHollow = false
    }

    /**
     * 处理边框拖动事件
     * return：边框有没有有效移动
     */
    fun handleHollowDrag(event: MotionEvent, dx: Int, dy: Int, refreshLastEventLinsnter: (MotionEvent) -> Unit): Boolean {
        hollowModel.let {
            when (it.selectSide) {
                HollowModel.LEFT -> {
                    val width = it.width - dx
                    if (width > it.initWidth * HOLLOW_SCALE_UPPER_LIMIT || width < it.initWidth * 0.5) {
                        //超出范围就不作处理

                        //使用回调函数
                        refreshLastEventLinsnter.invoke(event)
                        return false
                    }
                    val lastWidth = it.width
                    it.width = width
                    it.hollowX = it.hollowX + dx
                    setScaleWithCondition(scale * (it.width.toFloat() / lastWidth.toFloat()))
                }

                HollowModel.RIGHT -> {
                    val width = it.width + dx
                    if (width > it.initWidth * HOLLOW_SCALE_UPPER_LIMIT || width < it.initWidth * HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        refreshLastEventLinsnter.invoke(event)
                        return false
                    }
                    val lastWidth = it.width
                    it.width = it.width + dx
                    setScaleWithCondition(scale * (it.width.toFloat() / lastWidth.toFloat()))
                }

                HollowModel.TOP -> {
                    val height = it.height - dy
                    if (height > it.initWidth * HOLLOW_SCALE_UPPER_LIMIT || height < it.initWidth * HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        refreshLastEventLinsnter.invoke(event)
                        return false
                    }
                    val lastHeight = it.height
                    it.height = height
                    it.hollowY = it.hollowY + dy
                    setScaleWithCondition(scale * (it.height.toFloat() / lastHeight.toFloat()))
                }

                HollowModel.BOTTOM -> {
                    val height = it.height + dy
                    if (height > it.initWidth * HOLLOW_SCALE_UPPER_LIMIT || height < it.initWidth * HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        refreshLastEventLinsnter.invoke(event)
                        return false
                    }
                    val lastHeight = it.height
                    it.height = height

                    setScaleWithCondition(scale * (it.height.toFloat() / lastHeight.toFloat()))

                    Log.d("JigsawView", "HollowModel.height:${it.height}")
                    Log.d("JigsawView", "HollowModel dy:$dy")
                }

                else -> {
                    return false
                }
            }

            makePictureCropHollowWithoutAnimationIfNeed()
            refreshLastEventLinsnter.invoke(event)
        }

        return true
    }

    fun cancelHollowTouch(){
        isTouchHollow = false
    }

    /**
     * 移动图片到刚好填充边框区域，不使用动画
     *
     */
    private fun makePictureCropHollowWithoutAnimationIfNeed() {

            val hollowModel = hollowModel
            val bitmap = bitmapPicture
            val scale = scale
            val hollowX = hollowModel.hollowX
            val hollowY = hollowModel.hollowY
            val hollowWidth = hollowModel.width
            val hollowHeight = hollowModel.height

            val pictureLeft = (hollowX + xToHollowCenter + hollowWidth / 2 - bitmap.width / 2 * scale)
            val pictureTop = (hollowY + yToHollowCenter + hollowHeight / 2 - bitmap.height / 2 * scale)
            val pictureRight = (hollowX + xToHollowCenter + hollowWidth / 2 + bitmap.width / 2 * scale)
            val pictureBottom = (hollowY + yToHollowCenter + hollowHeight / 2 + bitmap.height / 2 * scale)

            val leftDiffer = pictureLeft - hollowX
            val topDiffer = pictureTop - hollowY
            val rightDiffer = pictureRight - (hollowX + hollowWidth)
            val bottomDiffer = pictureBottom - (hollowY + hollowHeight)

            if (leftDiffer > 0) {
                val targetXToHollow = (xToHollowCenter - leftDiffer).toInt()

                xToHollowCenter = targetXToHollow

                Log.d("JigsawView", "targetXToHollow:$targetXToHollow")
            }
            //图片上边进入边框内
            if (topDiffer > 0) {
                val targetYToHollow = (yToHollowCenter - topDiffer).toInt()
              yToHollowCenter = targetYToHollow

                Log.d("JigsawView", "targetYToHollow:$targetYToHollow")
            }
            //图片右边进入边框内
            if (rightDiffer < 0) {
                val targetXToHollow = (xToHollowCenter - rightDiffer).toInt()
                xToHollowCenter = targetXToHollow

                Log.d("JigsawView", "targetXToHollow:$targetXToHollow")
            }
            //图片低边进入边框内
            if (bottomDiffer < 0) {
                val targetYToHollow = (yToHollowCenter - bottomDiffer).toInt()

                yToHollowCenter = targetYToHollow

                Log.d("JigsawView", "targetYToHollow: $targetYToHollow")
            }

    }

}
