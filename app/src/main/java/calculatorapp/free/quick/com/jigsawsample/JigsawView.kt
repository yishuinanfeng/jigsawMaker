package calculatorapp.free.quick.com.jigsawsample

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import android.view.View

/**
 * 创建时间： 2019/4/2
 * 作者：yanyinan
 * 功能描述：
 */
class JigsawView(context: Context, private var mPictureModelList: List<PictureModel>) : View(context) {

    //绘制图片的画笔
    private val mPicturePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    //边框画笔
    private val mHollowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mMatrix = Matrix()

    private var mLastX: Float = 0.toFloat()
    private var mLastY: Float = 0.toFloat()

    private var mDownX: Float = 0.toFloat()
    private var mDownY: Float = 0.toFloat()

    private var mLastFingerDistance: Double = 0.toDouble()
    private var mTouchPictureModel: PictureModel? = null


    private var backgroundBitmap: Bitmap? = null
    private var doubleTouchMode:Boolean = false

    init {
        mHollowPaint.let {
            mHollowPaint.color = Color.RED
            mHollowPaint.strokeWidth = 2f
            mHollowPaint.style = Paint.Style.STROKE
        }
    }

    override fun onDraw(canvas: Canvas?) {
        //因为不支持wrap content，所以不需要重写onMeasure
        drawBackground(canvas)
        drawPicture(canvas)

    }

    private fun drawPicture(canvas: Canvas?) {

        canvas?.let { canvas ->
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

                    //图片的中点位置以边框区域中点为标准。由此算出缩放前图片平移后左上角坐标
                    val pictureX = hollowWidth / 2 - bitmap.width / 2 + it.xToHollowCenter
                    val pictureY = hollowHeight / 2 - bitmap.height / 2 + it.yToHollowCenter

                    mMatrix.postTranslate(pictureX.toFloat(), pictureY.toFloat())
                    mMatrix.postScale(scale, scale, (hollowWidth / 2 + it.xToHollowCenter).toFloat(), (hollowHeight / 2 + it.yToHollowCenter).toFloat())

                    canvas.clipRect(rect)
                    canvas.drawBitmap(bitmap, mMatrix, null)

                    drawHollow(canvas, hollowX, hollowY, rect)
                } else {

                }

                mMatrix.reset()
                canvas.restore()
            }
        }

    }

    fun drawHollow(canvas: Canvas, hollowX: Int, hollowY: Int, rect: Rect) {

        canvas.drawRect(rect, mHollowPaint)
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


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLastX = event.x
                mLastY = event.y

                mDownX = event.x
                mDownY = event.y

                Log.d("JigsawView", "mLastX:$mLastX")
                Log.d("JigsawView", "mLastY:$mLastY")

                mTouchPictureModel = getHandlePicModel(event)

                Log.d("JigsawView", "ACTION_DOWN pointerCount:${event.pointerCount}")

            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                //双指模式
                if (event.pointerCount == 2) {
                    doubleTouchMode = true
                    mTouchPictureModel = getHandlePicModel(event)

                    if (mTouchPictureModel != null) {
                        // mPicModelTouch.setSelect(true);
                        //   resetNoTouchPicsState();

                        //        mPicModelTouch.setSelect(true);

                        mLastFingerDistance = distanceBetweenFingers(event)

                        Log.d("JigsawView", "ACTION_POINTER_DOWN mLastFingerDistance:$mLastFingerDistance")

                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (event.pointerCount) {
                    1 -> {
                        if (doubleTouchMode){
                            return true
                        }

                        mTouchPictureModel?.let {
                            val dx = (event.x - mLastX).toInt()
                            val dy = (event.y - mLastY).toInt()
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
                            if (Math.abs(tempScale) < 3  || Math.abs(tempScale) > 0.5) {
                                it.scale = tempScale

                                invalidate()
                                mLastFingerDistance = fingerDistance

                                Log.d("JigsawView", "mLastFingerDistance:$mLastFingerDistance")
                            }

                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (doubleTouchMode){
                    doubleTouchMode = false
                }
                //空白部分动画归位
                makePictureCropHollowByTranslate(mTouchPictureModel)
            }

        }

        return true
    }

    /**
     * 移动图片到刚好填充边框区域
     */
    private fun makePictureCropHollowByTranslate(pictureModel: PictureModel?) {
        pictureModel?.let {
            val hollowModel = it.hollowModel
            val bitmap = it.bitmapPicture
            val scale = it.scale
            val hollowX = hollowModel.hollowX
            val hollowY = hollowModel.hollowY
            val hollowWidth = hollowModel.width
            val hollowHeight = hollowModel.height

            val pictureLeft = (hollowX + pictureModel.xToHollowCenter + hollowWidth / 2 - bitmap.width / 2 * scale)
            val pictureTop = (hollowY + pictureModel.yToHollowCenter + hollowHeight / 2 - bitmap.height / 2 * scale)
            val pictureRight = (hollowX + pictureModel.xToHollowCenter + hollowWidth / 2 + bitmap.width / 2 * scale)
            val pictureBottom = (hollowY + pictureModel.yToHollowCenter + hollowHeight / 2 + bitmap.height / 2 * scale)

            val leftDiffer = pictureLeft - hollowX
            val topDiffer = pictureTop - hollowY
            val rightDiffer = pictureRight - (hollowX + hollowWidth)
            val bottomDiffer = pictureBottom - (hollowY + hollowHeight)

            Log.d("JigsawView", "leftDiffer:$leftDiffer")
            Log.d("topDiffer", "topDiffer:$topDiffer")
            Log.d("rightDiffer", "rightDiffer:$rightDiffer")
            Log.d("JigsawView", "bottomDiffer:$bottomDiffer")

            if (leftDiffer > 0) {
                val targetXToHollow = (pictureModel.xToHollowCenter - leftDiffer).toInt()

                startCropHollowAnimation("PictureXToHollowCenter", pictureModel.xToHollowCenter, targetXToHollow)

                Log.d("JigsawView", "targetXToHollow:$targetXToHollow")
            }

            if (topDiffer > 0) {
                val targetYToHollow = (pictureModel.yToHollowCenter - topDiffer).toInt()

                startCropHollowAnimation("PictureYToHollowCenter", pictureModel.yToHollowCenter, targetYToHollow)

                Log.d("JigsawView", "targetYToHollow:$targetYToHollow")
            }

            if (rightDiffer < 0) {
                val targetXToHollow = (pictureModel.xToHollowCenter - rightDiffer).toInt()

                startCropHollowAnimation("PictureXToHollowCenter", pictureModel.xToHollowCenter, targetXToHollow)

                Log.d("JigsawView", "targetXToHollow:$targetXToHollow")
            }

            if (bottomDiffer < 0) {
                val targetYToHollow = (pictureModel.yToHollowCenter - bottomDiffer).toInt()

                startCropHollowAnimation("PictureYToHollowCenter", pictureModel.yToHollowCenter, targetYToHollow)

                Log.d("JigsawView", "targetYToHollow: $targetYToHollow")
            }
        }

        //  invalidate()

    }

    private fun startCropHollowAnimation(propertyName: String, picDistanceToHollow: Int, targetValue: Int) {
        val animator = ObjectAnimator.ofInt(this, propertyName, picDistanceToHollow, targetValue)
        animator.duration = 100
        animator.start()
    }

    /**
     * 为图片归位动画调用所用，不可混淆！
     */
    fun setPictureXToHollowCenter(x: Int) {
        mTouchPictureModel?.xToHollowCenter = x
        invalidate()

        Log.d("JigsawView", "setPictureXToHollowCenter: $x")
    }

    /**
     * 为图片归位动画调用所用，不可混淆！
     */
    fun setPictureYToHollowCenter(y: Int) {
        mTouchPictureModel?.yToHollowCenter = y
        invalidate()

        Log.d("JigsawView", "setPictureYToHollowCenter: $y")
    }

    /**
     * 根据事件点击区域得到对应的PictureModel，如果没有点击到图片所在区域则返回null
     *
     * @param event
     * @return
     */
    private fun getHandlePicModel(event: MotionEvent): PictureModel? {
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
        var scale: Float
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

}