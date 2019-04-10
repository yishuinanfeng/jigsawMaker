package calculatorapp.free.quick.com.jigsawsample

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View

/**
 * 创建时间： 2019/4/2
 * 作者：yanyinan
 * 功能描述：代表一张图片的模型。xToHollowCenter，yToHollowCenter为图片相对边框中点的偏移
 */
data class PictureModel(var bitmapPicture: Bitmap, val hollowModel: HollowModel, var xToHollowCenter: Int = 0, var yToHollowCenter: Int = 0) {

    companion object {
        private const val HOLLOW_TOUCH_WIDTH = 100
        private const val HOLLOW_SCALE_UPPER_LIMIT = 3
        private const val HOLLOW_TOUCH_LOWER_LIMIT = 100
        private const val PICTURE_ANIMATION_DELAY = 100L
    }

    var belongView: View? = null
    private var initScale: Float
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

    fun refreshStateWhenChangePic() {
        val hollowWidth = hollowModel.width
        val hollowHeight = hollowModel.height
        initScale = getCenterPicScale(bitmapPicture, hollowWidth, hollowHeight)
        scale = initScale
        xToHollowCenter = 0
        yToHollowCenter = 0
    }

    /**
     * 拖动当前model的currentModelDirection方向边，会带动modelList的model的targetDirection边变化
     * @param currentModelDirection：当前model的移动方向边
     */
    fun addEffectPictureModel(modelArray: SparseArray<List<PictureModel>>, currentModelDirection: Int) {
        mEffectPictureModel.put(currentModelDirection, modelArray)
    }

    private fun setScaleWithCondition(value: Float) {
        if (value < initScale) {
            scale = initScale
            return
        }
        if (value > HOLLOW_SCALE_UPPER_LIMIT * initScale) {
            return
        }
        scale = value
    }

    /**
     * 得到在固定的显示尺寸限定得Bitmap显示centerCrop效果的缩放比例(scale为图片和边框宽高比最大的值)
     */
    private fun getCenterPicScale(bitmap: Bitmap, width: Int, height: Int): Float {
        val widthBmp = bitmap.width
        val heightBmp = bitmap.height
        val widthScale = width.toFloat() / widthBmp.toFloat()
        val heightScale = height.toFloat() / heightBmp.toFloat()
        return if (widthScale < heightScale) {
            heightScale
        } else {
            widthScale
        }

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
    fun handleHollowDrag(event: MotionEvent, dx: Int, dy: Int, needEffectOthers: Boolean, overRangeListener: (MotionEvent) -> Unit): Boolean {
        hollowModel.let { model ->
            when (model.selectSide) {
                HollowModel.LEFT -> {
                    val width = model.width - dx
                    if (width < HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        overRangeListener.invoke(event)
                        return false
                    }

                    //联动其他的PictureModel
                    if (needEffectOthers) {
                        if (!handleEffectPictureModel(HollowModel.LEFT, event, dx, dy, overRangeListener)) {
                            //表示有一个联动的model已经到了最小值，不能再拖动边框
                            return false
                        }
                    }
                    val lastWidth = model.width
                    model.width = width
                    model.hollowX = model.hollowX + dx
                    setScaleWithCondition(scale * (model.width.toFloat() / lastWidth.toFloat()))

                }

                HollowModel.RIGHT -> {
                    val width = model.width + dx
                    if (width < HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        overRangeListener.invoke(event)
                        return false
                    }
                    //联动其他的PictureModel
                    if (needEffectOthers) {
                        if (!handleEffectPictureModel(HollowModel.RIGHT, event, dx, dy, overRangeListener)) {
                            //表示有一个联动的model已经到了最小值，不能再拖动边框
                            return false
                        }
                    }
                    val lastWidth = model.width
                    model.width = model.width + dx
                    setScaleWithCondition(scale * (model.width.toFloat() / lastWidth.toFloat()))

                }

                HollowModel.TOP -> {
                    val height = model.height - dy
                    if (height < HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        overRangeListener.invoke(event)
                        return false
                    }

                    if (needEffectOthers) {
                        if (!handleEffectPictureModel(HollowModel.TOP, event, dx, dy, overRangeListener)) {
                            //表示有一个联动的model已经到了最小值，不能再拖动边框
                            return false
                        }
                    }
                    val lastHeight = model.height
                    model.height = height
                    model.hollowY = model.hollowY + dy
                    setScaleWithCondition(scale * (model.height.toFloat() / lastHeight.toFloat()))


                }

                HollowModel.BOTTOM -> {
                    val height = model.height + dy
                    if (height < HOLLOW_TOUCH_LOWER_LIMIT) {
                        //超出范围就不作处理

                        //使用回调函数
                        overRangeListener.invoke(event)
                        return false
                    }

                    if (needEffectOthers) {
                        if (!handleEffectPictureModel(HollowModel.BOTTOM, event, dx, dy, overRangeListener)) {
                            //表示有一个联动的model已经到了最小值，不能再拖动边框
                            return false
                        }
                    }

                    val lastHeight = model.height
                    model.height = height

                    setScaleWithCondition(scale * (model.height.toFloat() / lastHeight.toFloat()))

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

    private fun handleEffectPictureModel(currentDirection: Int, event: MotionEvent, dx: Int, dy: Int, overRangeListener: (MotionEvent) -> Unit): Boolean {
        val modelArray = mEffectPictureModel.get(currentDirection)
        modelArray?.let { array ->
            val arraySize = array.size()
            for (i in 0 until arraySize) {
                val targetDirection = array.keyAt(i)
                val modelList = array.get(targetDirection)
                modelList?.forEach {
                    it.hollowModel.selectSide = targetDirection
                    //表示有一个联动的model已经到了最小值，不能再拖动边框(所以在添加联动model要注意，不同方向的边先添加，比如当前为左边，先添加联动到右边的model)
                    if (!it.handleHollowDrag(event, dx, dy, false, overRangeListener)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun cancelHollowTouch(pictureModel: PictureModel) {
        pictureModel.isTouchHollow = false
        pictureModel.hollowModel.selectSide = HollowModel.NO_SIDE

        //mEffectPictureModel所有联动的pictureModel重置状态
        val canHollowTouch = { model: PictureModel ->
            model.isTouchHollow = false
            model.hollowModel.selectSide = HollowModel.NO_SIDE
        }
        handleAllPictureModelByAction(canHollowTouch)
    }

    /**
     * 对所有需要联动的Picture实施某种操作
     */
    private fun handleAllPictureModelByAction(action: (PictureModel) -> Unit) {
        val arraySize = mEffectPictureModel.size()
        for (i in 0 until arraySize) {
            val keyArray = mEffectPictureModel.keyAt(i)
            val modelArray = mEffectPictureModel.get(keyArray)
            val modelSize = modelArray.size()
            for (j in 0 until modelSize) {
                val keyList = modelArray.keyAt(j)
                val modelList = modelArray.get(keyList)
                modelList.forEach {
                    action.invoke(it)
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

    fun backToCenterCropState(pictureModel: PictureModel, needEffectOthers: Boolean) {
        pictureModel.let {
            backToCenterCrop(it)

            if (!needEffectOthers) {
                return
            }
            val centerCrop = { model: PictureModel ->
                model.backToCenterCrop(model)
            }
            handleAllPictureModelByAction(centerCrop)
        }
    }

    private fun backToCenterCrop(it: PictureModel) {
        val hollowX = it.hollowModel.hollowX
        val hollowY = it.hollowModel.hollowY
        val hollowWidth = it.hollowModel.width
        val hollowHeight = it.hollowModel.height

        val pictureLeft = (hollowX + it.xToHollowCenter + hollowWidth / 2 - it.bitmapPicture.width / 2 * it.scale)
        val pictureTop = (hollowY + it.yToHollowCenter + hollowHeight / 2 - it.bitmapPicture.height / 2 * it.scale)
        val pictureRight = (hollowX + it.xToHollowCenter + hollowWidth / 2 + it.bitmapPicture.width / 2 * it.scale)
        val pictureBottom = (hollowY + it.yToHollowCenter + hollowHeight / 2 + it.bitmapPicture.height / 2 * it.scale)
        //四个方向的偏移
        val leftDiffer = pictureLeft - hollowX
        val topDiffer = pictureTop - hollowY
        val rightDiffer = pictureRight - (hollowX + hollowWidth)
        val bottomDiffer = pictureBottom - (hollowY + hollowHeight)
        //由偏移得到是否存在空白处
        if (leftDiffer > 0 || topDiffer > 0 || rightDiffer < 0 || bottomDiffer < 0) {
            val targetScale = getCenterPicScale(it.bitmapPicture, hollowWidth, hollowHeight)
            startAnimation("PictureScale", it.scale, targetScale)
            startAnimation("PictureXToHollowCenter", it.xToHollowCenter, 0)
            startAnimation("PictureYToHollowCenter", it.yToHollowCenter, 0)
        }
    }

    /**
     * 使用动画移动图片到刚好填充边框区域。仅用于单图拖动模式，不会联动其他图片。
     *
     */
    fun translatePictureCropHollowByAnimationIfNeed() {
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

        Log.d("JigsawView", "leftDiffer:$leftDiffer")
        Log.d("topDiffer", "topDiffer:$topDiffer")
        Log.d("rightDiffer", "rightDiffer:$rightDiffer")
        Log.d("JigsawView", "bottomDiffer:$bottomDiffer")
        //图片左边进入边框内
        if (leftDiffer > 0) {
            val targetXToHollow = (xToHollowCenter - leftDiffer).toInt()

            startAnimation("PictureXToHollowCenter", xToHollowCenter, targetXToHollow)

            Log.d("JigsawView", "targetXToHollow:$targetXToHollow")
        }
        //图片上边进入边框内
        if (topDiffer > 0) {
            val targetYToHollow = (yToHollowCenter - topDiffer).toInt()

            startAnimation("PictureYToHollowCenter", yToHollowCenter, targetYToHollow)

            Log.d("JigsawView", "targetYToHollow:$targetYToHollow")
        }
        //图片右边进入边框内
        if (rightDiffer < 0) {
            val targetXToHollow = (xToHollowCenter - rightDiffer).toInt()

            startAnimation("PictureXToHollowCenter", xToHollowCenter, targetXToHollow)

            Log.d("JigsawView", "targetXToHollow:$targetXToHollow")
        }
        //图片低边进入边框内
        if (bottomDiffer < 0) {
            val targetYToHollow = (yToHollowCenter - bottomDiffer).toInt()

            startAnimation("PictureYToHollowCenter", yToHollowCenter, targetYToHollow)

            Log.d("JigsawView", "targetYToHollow: $targetYToHollow")
        }

    }

    /**
     * 缩放状态下动画回到cnterCrop状态以填充空白
     */
    fun backToCenterCropStateIfNeed() {
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

        if (leftDiffer > 0 || topDiffer > 0 || rightDiffer < 0 || bottomDiffer < 0) {
            val targetScale = getCenterPicScale(bitmap, hollowWidth, hollowHeight)
            startAnimation("PictureScale", scale, targetScale)
            startAnimation("PictureXToHollowCenter", xToHollowCenter, 0)
            startAnimation("PictureYToHollowCenter", yToHollowCenter, 0)
        }

    }

    private fun startAnimation(propertyName: String, initValue: Int, targetValue: Int) {
        val animator = ObjectAnimator.ofInt(this, propertyName, initValue, targetValue)
        animator.duration = PICTURE_ANIMATION_DELAY
        animator.start()
    }

    private fun startAnimation(propertyName: String, initValue: Float, targetValue: Float) {
        val animator = ObjectAnimator.ofFloat(this, propertyName, initValue, targetValue)
        animator.duration = PICTURE_ANIMATION_DELAY
        animator.start()
    }

    /**
     * 为图片归位动画调用所用，不可混淆！
     *
     */
    fun setPictureXToHollowCenter(x: Int) {
        xToHollowCenter = x
        belongView?.invalidate()

        Log.d("PictureModel", "setPictureXToHollowCenter: $x")
    }

    /**
     * 为图片归位动画调用所用，不可混淆！
     *
     */
    fun setPictureYToHollowCenter(y: Int) {
        yToHollowCenter = y
        belongView?.invalidate()

        Log.d("PictureModel", "setPictureYToHollowCenter: $y")
    }

    /**
     * 为图片归位动画调用所用，不可混淆！
     *
     */
    fun setPictureScale(scale: Float) {
        this.scale = scale
        belongView?.invalidate()

        Log.d("PictureModel", "setScale: $scale")
    }

}
