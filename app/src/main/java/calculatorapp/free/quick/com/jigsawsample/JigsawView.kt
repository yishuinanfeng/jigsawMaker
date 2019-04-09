package calculatorapp.free.quick.com.jigsawsample

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * 创建时间： 2019/4/2
 * 作者：yanyinan
 * 功能描述：一个多拼图可拖拽缩放、边框的View。
 * 注意：1.暂时不支持在xml文件中定义 2.布局不支持wrap content
 */
class JigsawView(context: Context, private var mPictureModelList: List<PictureModel>) : View(context) {

    companion object {
        private const val HOLLOW_SCALE_UPPER_LIMIT = 1.5
        private const val HOLLOW_TOUCH_LOWER_LIMIT = 0.5
        private const val PICTURE_ANIMATION_DELAY = 100L

    }

    init {
        mPictureModelList.forEach {
            it.belongView = this
        }
    }

    //绘制图片的画笔
    private val mPictureHalfAlphaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    //边框画笔
    private val mHollowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mHollowSelectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    //图片变换使用一个Matrix对象的多次创建
    private val mMatrix = Matrix()

    private var mLastX: Float = 0.toFloat()
    private var mLastY: Float = 0.toFloat()

    private var mDownX: Float = 0.toFloat()
    private var mDownY: Float = 0.toFloat()

    private var mLastFingerDistance: Double = 0.toDouble()
    private var mTouchPictureModel: PictureModel? = null
    //private var mTouchHollowModel: HollowModel? = null

    private var backgroundBitmap: Bitmap? = null
    private var doubleTouchMode: Boolean = false

    private val viewConfig: ViewConfiguration

    private var isNeedDrawShadow = false

    init {
        mHollowPaint.color = Color.RED
        mHollowPaint.strokeWidth = 2f
        mHollowPaint.style = Paint.Style.STROKE

        mHollowSelectPaint.color = Color.RED
        mHollowSelectPaint.strokeWidth = 10f
        mHollowSelectPaint.style = Paint.Style.STROKE

        mPictureHalfAlphaPaint.alpha = 80

        viewConfig = ViewConfiguration.get(context)
    }

    override fun onDraw(canvas: Canvas?) {
        //因为不支持wrap content，所以不需要重写onMeasure
        drawBackground(canvas)
        drawPicture(canvas)

    }

    private fun drawPicture(canvas: Canvas?) {

        canvas?.let { canvas ->

            var matrixShadow: Matrix
            var canvasShadow: Canvas
            var modelShadow: PictureModel


            mPictureModelList.forEach {
                canvas.save()

                val scale = it.scale

                Log.d("JigsawView", "scale:$scale")
                val bitmap = it.bitmapPicture

                val hollowModel = it.hollowModel
                val hollowX = hollowModel.hollowX
                val hollowY = hollowModel.hollowY
                val hollowWidth = hollowModel.width
                val hollowHeight = hollowModel.height
                val hollowPath = hollowModel.path

                if (hollowPath == null) {
                    val rect = Rect(0, 0, hollowWidth, hollowHeight)
                    canvas.translate(hollowX.toFloat(), hollowY.toFloat())

                    //图片的中点位置以边框区域中点为标准。根据图片大小以及图片中心点边框区域中点的偏移距离和算出缩放前图片平移后左上角坐标
                    val pictureX = hollowWidth / 2 - bitmap.width / 2 + it.xToHollowCenter
                    val pictureY = hollowHeight / 2 - bitmap.height / 2 + it.yToHollowCenter

                    mMatrix.postTranslate(pictureX.toFloat(), pictureY.toFloat())
                    mMatrix.postScale(scale, scale, (hollowWidth / 2 + it.xToHollowCenter).toFloat(), (hollowHeight / 2 + it.yToHollowCenter).toFloat())

                    canvas.clipRect(rect)
                    canvas.drawBitmap(bitmap, mMatrix, null)
                    drawHollow(canvas, hollowX, hollowY, rect, it.isSelected)

                    if (it.isSelected && isNeedDrawShadow) {
                        canvas.drawBitmap(bitmap, mMatrix, mPictureHalfAlphaPaint)
                    }
                    canvas.restore()

                } else {

                }

                mMatrix.reset()

            }

            drawPictureShadow(canvas)
        }

    }

    /**
     * 绘制选中的图片的虚影
     */
    private fun drawPictureShadow(canvas: Canvas?) {
        mTouchPictureModel?.let {
            canvas?.save()

            val scale = it.scale

            Log.d("JigsawView", "scale:$scale")
            val bitmap = it.bitmapPicture

            val hollowModel = it.hollowModel
            val hollowX = hollowModel.hollowX
            val hollowY = hollowModel.hollowY
            val hollowWidth = hollowModel.width
            val hollowHeight = hollowModel.height
            val hollowPath = hollowModel.path

            if (hollowPath == null) {
                canvas?.translate(hollowX.toFloat(), hollowY.toFloat())
                //图片的中点位置以边框区域中点为标准。根据图片大小以及图片中心点边框区域中点的偏移距离和算出缩放前图片平移后左上角坐标
                val pictureX = hollowWidth / 2 - bitmap.width / 2 + it.xToHollowCenter
                val pictureY = hollowHeight / 2 - bitmap.height / 2 + it.yToHollowCenter

                mMatrix.postTranslate(pictureX.toFloat(), pictureY.toFloat())
                mMatrix.postScale(scale, scale, (hollowWidth / 2 + it.xToHollowCenter).toFloat(), (hollowHeight / 2 + it.yToHollowCenter).toFloat())

                if (it.isSelected && isNeedDrawShadow) {
                    canvas?.drawBitmap(bitmap, mMatrix, mPictureHalfAlphaPaint)
                }
                canvas?.restore()
            }
        }
        mMatrix.reset()
    }

    private fun drawHollow(canvas: Canvas, hollowX: Int, hollowY: Int, rect: Rect, selected: Boolean) {
        if (selected) {
            canvas.drawRect(rect, mHollowSelectPaint)
        } else {
            canvas.drawRect(rect, mHollowPaint)
        }
    }

    private fun drawBackground(canvas: Canvas?) {
        backgroundBitmap?.let { backgroundBitmap ->
            val scale = getCenterPicScale(backgroundBitmap, width, height)
            mMatrix.setScale(scale, scale)
            canvas?.let {
                it.save()
                it.translate((width / 2).toFloat(), (height / 2).toFloat())
                it.drawBitmap(backgroundBitmap, mMatrix, null)
                it.restore()
                mMatrix.reset()
            }
        }
    }

    private var refreshLastEventListener: (MotionEvent) -> Unit = { event ->
        mLastX = event.x
        mLastY = event.y
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isNeedDrawShadow = true

                mLastX = event.x
                mLastY = event.y

                mDownX = event.x
                mDownY = event.y

                Log.d("JigsawView", "mLastX:$mLastX")
                Log.d("JigsawView", "mLastY:$mLastY")

                mTouchPictureModel = getTouchPicModel(event)

                // mTouchHollowModel = getTouchHollowModel(event, mTouchPictureModel)
                mTouchPictureModel?.let {
                    it.refreshIsTouchHollowState(event)
                }

                selectPictureModel()
                invalidate()

                Log.d("JigsawView", "ACTION_DOWN pointerCount:${event.pointerCount}")
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                //双指模式
                if (event.pointerCount == 2) {
                    isNeedDrawShadow = true

                    doubleTouchMode = true
                    mTouchPictureModel = getTouchPicModel(event)
                    //mTouchHollowModel = null

                    if (mTouchPictureModel != null) {
                        mLastFingerDistance = distanceBetweenFingers(event)

                        Log.d("JigsawView", "ACTION_POINTER_DOWN mLastFingerDistance:$mLastFingerDistance")

                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (event.pointerCount) {
                    1 -> {
                        if (doubleTouchMode) {
                            return true
                        }

                        val dx = (event.x - mLastX).toInt()
                        val dy = (event.y - mLastY).toInt()

                        Log.d("JigsawView", "HollowModel dy:$dy")

                        mTouchPictureModel?.let { pictureModel ->
                            if (pictureModel.isTouchHollow) {
                                if (pictureModel.handleHollowDrag(event, dx, dy, true, refreshLastEventListener)) {
                                    invalidate()

                                }

                                //对边框处理过就不需要对图片进行处理了
                                return true
                            }
                        }

                        mTouchPictureModel?.let {

                            it.xToHollowCenter = it.xToHollowCenter + dx
                            it.yToHollowCenter = it.yToHollowCenter + dy

                            Log.d("JigsawView", "dx:$dx")
                            Log.d("JigsawView", "dy:$dy")
                            invalidate()
                        }

                        mLastX = event.x
                        mLastY = event.y
                    }

                    2 -> {
                        mTouchPictureModel?.let {
                            val fingerDistance = distanceBetweenFingers(event)
                            //当前手指距离和上一次的手指距离的比即为图片缩放比
                            val scaleRatioDelta = fingerDistance.toFloat() / mLastFingerDistance.toFloat()

                            Log.d("JigsawView", "scaleRatioDelta:$scaleRatioDelta")

                            val tempScale = scaleRatioDelta * it.scale

                            //对缩放比做限制
                            if (Math.abs(tempScale) < 3 || Math.abs(tempScale) > 0.5) {
                                it.scale = tempScale

                                invalidate()
                                mLastFingerDistance = fingerDistance

                                Log.d("JigsawView", "mLastFingerDistance:$mLastFingerDistance")
                            }

                        }
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                val distanceFromDownPoint = getDisFromDownPoint(event)
//                if (distanceFromDownPoint < viewConfig.scaledTouchSlop) {
//                    //选中状态
//                    selectPictureModel()
//                    invalidate()
//                    return true
//                }
                isNeedDrawShadow = false

                if (doubleTouchMode) {
                    //缩放图片导致的边框内空白部分动画放大图片填充
                    mTouchPictureModel?.backToCenterCropStateIfNeed()
                } else {
                    mTouchPictureModel?.let {
                        if (!it.isTouchHollow) {
                            //不是拖动边框，而是拖动图片则动画填充
                            it.translatePictureCropHollowByAnimationIfNeed()
                        } else {
                            //拖动边框如果此时有空白则centercrop填充
                            postDelayed({
                                it.backToCenterCropStateWithAllEffectPic(it)
                            }, PICTURE_ANIMATION_DELAY + 10)
                        }
                    }

                }

                mTouchPictureModel?.cancelHollowTouch(mTouchPictureModel!!)

                if (doubleTouchMode) {
                    doubleTouchMode = false
                }

                invalidate()
            }
        }

        return true
    }

    private fun selectPictureModel() {
        mPictureModelList.forEach {
            it.isSelected = false
        }
        mTouchPictureModel?.isSelected = true
    }

    /**
     * 根据事件点击区域得到被点击到的PictureModel，如果没有点击图片则返回null
     *
     * @param event
     * @return
     */
    private fun getTouchPicModel(event: MotionEvent): PictureModel? {
        when (event.pointerCount) {
            1 -> {
                val x = event.x.toInt()
                val y = event.y.toInt()
                for (picModel in mPictureModelList) {
                    val hollowX = picModel.hollowModel.hollowX
                    val hollowY = picModel.hollowModel.hollowY
                    val hollowWidth = picModel.hollowModel.width
                    val hollowHeight = picModel.hollowModel.height

                    val rect = Rect(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight)
                    //点在矩形区域中
                    if (rect.contains(x, y)) {
                        return picModel
                    }
                }
            }
            2 -> {
                val x0 = event.getX(0).toInt()
                val y0 = event.getY(0).toInt()
                val x1 = event.getX(1).toInt()
                val y1 = event.getY(1).toInt()
                for (picModel in mPictureModelList) {
                    val hollowX = picModel.hollowModel.hollowX
                    val hollowY = picModel.hollowModel.hollowY
                    val hollowWidth = picModel.hollowModel.width
                    val hollowHeight = picModel.hollowModel.height

                    val rect = Rect(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight)
                    //两个点都在该矩形区域
                    if (rect.contains(x0, y0) || rect.contains(x1, y1)) {
                        return picModel
                    }
                }
            }
            else -> {
            }
        }
        return null
    }

    /**
     * 修改背景画布
     */
    public fun setBackground(background: Bitmap) {
        backgroundBitmap = background
        invalidate()
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

        matrix.setScale(scale, scale)
        return scale
    }

    /**
     * 计算两个手指之间的距离。
     *
     * @param event
     * @return 两个手指之间的距离
     */
    private fun distanceBetweenFingers(event: MotionEvent): Double {
        val disX = Math.abs(event.getX(0) - event.getX(1))
        val disY = Math.abs(event.getY(0) - event.getY(1))
        return Math.sqrt((disX * disX + disY * disY).toDouble())
    }

    private fun getDisFromDownPoint(event: MotionEvent): Double {
        val disX = Math.abs(event.x - mDownX)
        val disY = Math.abs(event.y - mDownY)
        return Math.sqrt((disX * disX + disY * disY).toDouble())
    }

}