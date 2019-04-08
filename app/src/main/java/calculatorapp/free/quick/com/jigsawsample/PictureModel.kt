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
     * 指定了当前PictureModel可移动的边框以及会被当前的PictureModel边框的拖动影响到的其他PictureModel
     *
     * 外层Map的key：当前PictureModel所能拖动的边框。使用HollowModel的Left/Top/Right/bottom表示
     * 内层Map:Value为被当前PictureModel所联动的PictureModel集合，Key为被当前PictureModel所联动的PictureModel集合中的PictureModel需要移动的边框（即拖动当前的PictureModel会影响到的边）
     * ，使用HollowModel的Left/Top/Right/bottom表示
     */
    private val mEffectPictureModel: SparseArray<SparseArray<List<PictureModel>>> = SparseArray()

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

    /**
     * 拖动当前model的currentModelDirection方向边，会带动modelList的model的targetDirection边变化
     * @param targetDirection:被联动的model的移动方向边 currentModelDirection：当前model的移动方向边
     */
    fun addEffectPictureModel(modelArray:SparseArray<List<PictureModel>>, currentModelDirection: Int) {
        mEffectPictureModel.put(currentModelDirection, modelArray)
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
        if (rectLeft.contains(x, y) && mEffectPictureModel.get(HollowModel.LEFT) != null) {
            hollowModel.selectSide = HollowModel.LEFT
            isTouchHollow = true
        }
        if (rectTop.contains(x, y) && mEffectPictureModel.get(HollowModel.TOP) != null) {
            hollowModel.selectSide = HollowModel.TOP
            isTouchHollow = true
        }
        if (rectRight.contains(x, y) && mEffectPictureModel.get(HollowModel.RIGHT) != null) {
            hollowModel.selectSide = HollowModel.RIGHT
            isTouchHollow = true
        }
        if (rectBottom.contains(x, y) && mEffectPictureModel.get(HollowModel.BOTTOM) != null) {
            hollowModel.selectSide = HollowModel.BOTTOM
            isTouchHollow = true
        }

    }

    /**
     * 处理边框拖动事件
     * return：边框有没有有效移动
     */
    fun handleHollowDrag(event: MotionEvent, dx: Int, dy: Int, overRangeListener: (MotionEvent) -> Unit): Boolean {
        hollowModel.let { model ->
            when (model.selectSide) {
                HollowModel.LEFT -> {
                    val width = model.width - dx
                    if (width > model.initWidth * HOLLOW_SCALE_UPPER_LIMIT || width < model.initWidth * 0.5) {
                        //超出范围就不作处理

                        //使用回调函数
                        overRangeListener.invoke(event)
                        return false
                    }
                    val lastWidth = model.width
                    model.width = width
                    model.hollowX = model.hollowX + dx
                    setScaleWithCondition(scale * (model.width.toFloat() / lastWidth.toFloat()))

                    //联动其他的PictureModel
                    handleEffectPictureModel(HollowModel.LEFT, event, dx, dy, overRangeListener)
                }

                HollowModel.RIGHT -> {
                    val width = model.width + dx
                    if (width > model.initWidth * HOLLOW_SCALE_UPPER_LIMIT || width < model.initWidth * HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        overRangeListener.invoke(event)
                        return false
                    }
                    val lastWidth = model.width
                    model.width = model.width + dx
                    setScaleWithCondition(scale * (model.width.toFloat() / lastWidth.toFloat()))

                    //联动其他的PictureModel
                    handleEffectPictureModel(HollowModel.RIGHT, event, dx, dy, overRangeListener)
                }

                HollowModel.TOP -> {
                    val height = model.height - dy
                    if (height > model.initHeight * HOLLOW_SCALE_UPPER_LIMIT || height < model.initHeight * HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        overRangeListener.invoke(event)
                        return false
                    }
                    val lastHeight = model.height
                    model.height = height
                    model.hollowY = model.hollowY + dy
                    setScaleWithCondition(scale * (model.height.toFloat() / lastHeight.toFloat()))

                    handleEffectPictureModel(HollowModel.TOP, event, dx, dy, overRangeListener)
                }

                HollowModel.BOTTOM -> {
                    val height = model.height + dy
                    if (height > model.initHeight * HOLLOW_SCALE_UPPER_LIMIT || height < model.initHeight * HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        overRangeListener.invoke(event)
                        return false
                    }
                    val lastHeight = model.height
                    model.height = height

                    setScaleWithCondition(scale * (model.height.toFloat() / lastHeight.toFloat()))

                    handleEffectPictureModel(HollowModel.BOTTOM, event, dx, dy, overRangeListener)

                    Log.d("JigsawView", "HollowModel.height:${model.height}")
                    Log.d("JigsawView", "HollowModel dy:$dy")
                }

                else -> {
                    return false
                }
            }

            makePictureCropHollowWithoutAnimationIfNeed()
            overRangeListener.invoke(event)
        }

        return true
    }

    private fun handleEffectPictureModel(currentDirection: Int, event: MotionEvent, dx: Int, dy: Int, overRangeListener: (MotionEvent) -> Unit) {
        val modelArray = mEffectPictureModel.get(currentDirection)
        modelArray?.let { array ->
            val arraySize = array.size()
            for (i in 0 until arraySize) {
                val targetDirection = array.keyAt(i)
                val modelList = array.get(targetDirection)
                modelList?.forEach {
                    it.hollowModel.selectSide = targetDirection
                    it.handleHollowDrag(event, dx, dy, overRangeListener)
                }
            }
        }

    }

    fun cancelHollowTouch() {
        isTouchHollow = false
        hollowModel.selectSide = HollowModel.NO_SIDE

        //mEffectPictureModel所有联动的pictureModel重置状态
        val arraySize = mEffectPictureModel.size()
        for (i in 0 until arraySize) {
            val keyArray = mEffectPictureModel.keyAt(i)
            val modelArray = mEffectPictureModel.get(keyArray)
            val modelSize = modelArray.size()
            for (j in 0 until modelSize) {
                val keyList = modelArray.keyAt(j)
                val modelList = modelArray.get(keyList)
                modelList.forEach {
                    it.cancelHollowTouch()
                }
            }
        }
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
