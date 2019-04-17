package calculatorapp.free.quick.com.jigsawsample

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.graphics.RectF


/**
 * 创建时间： 2019/4/2
 * 作者：yanyinan
 * 功能描述：一个多拼图可拖拽缩放、边框的View。
 * 注意：1.暂时不支持在xml文件中定义 2.布局不支持wrap content
 * @param heightWidthRatio 高度比宽度
 */
class JigsawView(context: Context, isRegular: Boolean) : View(context) {
//    private val mHeightWidthRatio: Float = heightWidthRatio

    companion object {
        private val TAG = JigsawView::class.java.simpleName
        private const val GAP_MAX = 20
        private const val ROUND_RADIUS_MAX = 10
        private const val PICTURE_ANIMATION_DELAY = 100L
        private const val SELECT_DRAG_RECT_LENGTH = 150
        private const val SELECT_DRAG_RECT_WIDTH = 20
    }

    private val mIsRegular = isRegular
    private var mPictureModelList = mutableListOf<PictureModel>()

    //绘制半透明（虚影）图片的画笔
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
    /**
     * 双指触摸模式
     */
    private var doubleTouchMode: Boolean = false

    private val viewConfig: ViewConfiguration
    private var downTime: Long = 0
    /**
     * 需要绘制图片虚影
     */
    private var isNeedDrawShadow = false
    private lateinit var runnable: Runnable
    private val longClickHandler = DelayHandler()
    /**
     * 交换图片模式
     */
    private var changePicMode = false
    private var willChangeModel: PictureModel? = null
    /**
     * 内边距
     */
    private var hollowGap = 0f
    private var lastPicGap = hollowGap
    /**
     * 边框的圆角半径
     */
    private var hollowRoundRadius = 10.0f

    fun initPictureModelList(pictureModelList: List<PictureModel>) {
        mPictureModelList.clear()
        mPictureModelList.addAll(pictureModelList)
        mPictureModelList.forEach {
            it.belongView = this
        }

        invalidate()
    }


    fun setGap(gap: Float) {
        val differGap = gap - lastPicGap
        mPictureModelList.forEach {
            val hollow = it.hollowModel
            hollow.hollowX = hollow.hollowX + differGap * GAP_MAX
            hollow.hollowY = hollow.hollowY + differGap * GAP_MAX
            hollow.width = hollow.width - differGap * GAP_MAX * 2
            hollow.height = hollow.height - differGap * GAP_MAX * 2

            if (it.isSelected)
                Log.d(TAG, "hollow hollowX:${hollow.hollowX}, hollowY:${hollow.hollowY},hollow.width:${hollow.width},hollow.height:${hollow.height}")
        }
        lastPicGap = gap
        invalidate()
    }

    fun setHollowRoundRadius(radius: Float) {
        this.hollowRoundRadius = radius * ROUND_RADIUS_MAX
        invalidate()
    }

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
        canvas?.drawColor(Color.YELLOW)
        drawBackground(canvas)
        drawPicture(canvas)
    }

//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val width = MeasureSpec.getSize(widthMeasureSpec)
//        val height = width * mHeightWidthRatio
//        setMeasuredDimension(width, height.toInt())
//    }

    private fun drawPicture(canvas: Canvas?) {

        canvas?.let { canvas ->
            mPictureModelList.forEach {

                //                if (it.isSelected && !changePicMode) {
//                    Log.d("JigsawView", "setPictureXToHollowCenter: ${it.xToHollowCenter}")
//                    Log.d("JigsawView", "setPictureYToHollowCenter: ${it.yToHollowCenter}")
//                    return@forEach
//                }
                canvas.save()

                val scaleX = it.scaleX
                val scaleY = it.scaleY

                //  Log.d("JigsawView", "scaleX:$scale")
                val bitmap = it.bitmapPicture

                val hollowModel = it.hollowModel
                val hollowX = hollowModel.hollowX
                val hollowY = hollowModel.hollowY
                val hollowWidth = hollowModel.width
                val hollowHeight = hollowModel.height
                val hollowPath = hollowModel.path

                if (mIsRegular && hollowPath == null) {
                    //  val rect = RectF(hollowGap * GAP_MAX, hollowGap * GAP_MAX, (hollowWidth - hollowGap * GAP_MAX), (hollowHeight - hollowGap * GAP_MAX))
                    val rect = RectF(0f, 0f, hollowWidth, hollowHeight)

                    if (!it.isSelected || !changePicMode) {
                        Log.d("JigsawView", "setPictureXToHollowCenter: ${it.xToHollowCenter}")
                        Log.d("JigsawView", "setPictureYToHollowCenter: ${it.yToHollowCenter}")


                        //规则图形，可以拖动边框并联动其他图形
                        //Path使用绝对值点的话，本行应该移动到canvas.clip后面
                        canvas.translate(hollowX, hollowY)

                        //图片的中点位置以边框区域中点为标准。根据图片大小以及图片中心点边框区域中点的偏移距离和算出缩放前图片平移后左上角坐标
                        val pictureX = hollowWidth / 2 - bitmap.width / 2 + it.xToHollowCenter
                        val pictureY = hollowHeight / 2 - bitmap.height / 2 + it.yToHollowCenter

                        mMatrix.postTranslate(pictureX, pictureY)
                        mMatrix.postScale(scaleX, scaleY, (hollowWidth / 2 + it.xToHollowCenter), (hollowHeight / 2 + it.yToHollowCenter))

                        mMatrix.postRotate(it.rotateDegree, (hollowWidth / 2 + it.xToHollowCenter), (hollowHeight / 2 + it.yToHollowCenter))

                        val clipPath = Path()
                        clipPath.addRoundRect(rect, hollowRoundRadius, hollowRoundRadius, Path.Direction.CW)
                        canvas.clipPath(clipPath)

                        //准备交换的图片
                        if (changePicMode && !it.isSelected && it == willChangeModel) {
                            canvas.drawBitmap(bitmap, mMatrix, mPictureHalfAlphaPaint)
                        } else {
                            canvas.drawBitmap(bitmap, mMatrix, null)
                        }
                    }

                    mMatrix.reset()
                    canvas.restore()

                    // drawHollowWithDragSign(it, canvas)

                } else {
                    //不规则图形，不可以拖动边框并联动其他图形
                    if (!it.isSelected || !changePicMode) {
                        val scalePathGap = getPathScale()
                        canvas.scale(scalePathGap, scalePathGap, it.hollowModel.centerPoint!!.x.toFloat(), it.hollowModel.centerPoint.y.toFloat())
                        canvas.clipPath(hollowPath!!)
                        canvas.scale(1 / scalePathGap, 1 / scalePathGap, it.hollowModel.centerPoint.x.toFloat(), it.hollowModel.centerPoint.y.toFloat())
                        //Path使用绝对值点的话，本行应该移动到canvas.clip后面
                        canvas.translate(hollowX, hollowY)

                        //图片的中点位置以边框区域中点为标准。根据图片大小以及图片中心点边框区域中点的偏移距离和算出缩放前图片平移后左上角坐标
                        val pictureX = hollowWidth / 2 - bitmap.width / 2 + it.xToHollowCenter
                        val pictureY = hollowHeight / 2 - bitmap.height / 2 + it.yToHollowCenter

                        mMatrix.postTranslate(pictureX, pictureY)
                        //    scalePicWithGap(hollowWidth, it, hollowHeight)
                        mMatrix.postScale(scaleX, scaleY, (hollowWidth / 2 + it.xToHollowCenter).toFloat(), (hollowHeight / 2 + it.yToHollowCenter).toFloat())
                        mMatrix.postRotate(it.rotateDegree, (hollowWidth / 2 + it.xToHollowCenter).toFloat(), (hollowHeight / 2 + it.yToHollowCenter).toFloat())

                        //准备交换的图片
                        if (changePicMode && !it.isSelected && it == willChangeModel) {
                            canvas.drawBitmap(bitmap, mMatrix, mPictureHalfAlphaPaint)
                        } else {
                            canvas.drawBitmap(bitmap, mMatrix, null)
                        }
                    }

                    mMatrix.reset()
                    canvas.restore()

                    //绘制path
                    Log.d(TAG, "isSelected : ${it.isSelected}")
                    if (it.isSelected) {
                        canvas.save()
                        val scalePath = getPathScale()
                        canvas.scale(scalePath, scalePath, it.hollowModel.centerPoint!!.x.toFloat(), it.hollowModel.centerPoint.y.toFloat())
                        canvas.drawPath(hollowPath!!, mHollowSelectPaint)
                        canvas.restore()
                    }
                }
            }

            mTouchPictureModel?.let {
                drawHollowWithDragSign(it, canvas)
            }

            drawPictureShadow(canvas)
        }

    }

    private fun drawHollowWithDragSign(it: PictureModel, canvas: Canvas) {
        if (!mIsRegular) {
            return
        }
        if (it.isSelected) {
            val rect = RectF(it.hollowModel.hollowX, it.hollowModel.hollowY, it.hollowModel.hollowX + it.hollowModel.width
                    , it.hollowModel.hollowY + it.hollowModel.height)
            canvas.drawRoundRect(rect, hollowRoundRadius, hollowRoundRadius, mHollowSelectPaint)

            //根据PictureModel的mCanDragDirectionList画出对应的可拖拽矩形标志
            val canDragList = it.getCanDragList()
            canDragList.forEach { direction ->
                when (direction) {
                    HollowModel.LEFT -> {
                        val rectLeft = RectF((it.hollowModel.hollowX - SELECT_DRAG_RECT_WIDTH / 2).toFloat()
                                , (it.hollowModel.hollowY + it.hollowModel.height / 2 - SELECT_DRAG_RECT_LENGTH / 2).toFloat()
                                , (it.hollowModel.hollowY + it.hollowModel.hollowX + SELECT_DRAG_RECT_WIDTH / 2).toFloat()
                                , (it.hollowModel.hollowY + it.hollowModel.height / 2 + SELECT_DRAG_RECT_LENGTH / 2).toFloat())
                        canvas.drawRoundRect(rectLeft, hollowRoundRadius, hollowRoundRadius, mHollowSelectPaint)
                    }
                    HollowModel.TOP -> {
                        val rectLeft = RectF((it.hollowModel.hollowX + it.hollowModel.width / 2 - SELECT_DRAG_RECT_LENGTH / 2).toFloat()
                                , (it.hollowModel.hollowY - SELECT_DRAG_RECT_WIDTH / 2).toFloat()
                                , (it.hollowModel.hollowX + it.hollowModel.width / 2 + SELECT_DRAG_RECT_LENGTH / 2).toFloat()
                                , (it.hollowModel.hollowY + SELECT_DRAG_RECT_WIDTH / 2).toFloat())
                        canvas.drawRoundRect(rectLeft, hollowRoundRadius, hollowRoundRadius, mHollowSelectPaint)
                    }
                    HollowModel.RIGHT -> {
                        val rectLeft = RectF((it.hollowModel.hollowX + it.hollowModel.width - SELECT_DRAG_RECT_WIDTH / 2).toFloat()
                                , (it.hollowModel.hollowY + it.hollowModel.height / 2 - SELECT_DRAG_RECT_LENGTH / 2).toFloat()
                                , (it.hollowModel.hollowX + it.hollowModel.width + SELECT_DRAG_RECT_WIDTH / 2).toFloat()
                                , (it.hollowModel.hollowY + it.hollowModel.height / 2 + SELECT_DRAG_RECT_LENGTH / 2).toFloat())
                        canvas.drawRoundRect(rectLeft, hollowRoundRadius, hollowRoundRadius, mHollowSelectPaint)

                    }
                    HollowModel.BOTTOM -> {
                        val rectLeft = RectF((it.hollowModel.hollowX + it.hollowModel.width / 2 - SELECT_DRAG_RECT_LENGTH / 2).toFloat()
                                , (it.hollowModel.hollowY + it.hollowModel.height - SELECT_DRAG_RECT_WIDTH / 2).toFloat()
                                , (it.hollowModel.hollowX + it.hollowModel.width / 2 + SELECT_DRAG_RECT_LENGTH / 2).toFloat()
                                , (it.hollowModel.hollowY + it.hollowModel.height + SELECT_DRAG_RECT_WIDTH / 2).toFloat())
                        canvas.drawRoundRect(rectLeft, hollowRoundRadius, hollowRoundRadius, mHollowSelectPaint)
                    }
                }
            }
        }
    }

    private fun getPathScale(): Float {
        return 1 - lastPicGap * 0.2f
    }

    fun overTurnHorizontal() {
        mTouchPictureModel?.overTurnHorizontal()
        invalidate()
    }

    fun overTurnVertical() {
        mTouchPictureModel?.overTurnVertical()
        invalidate()
    }

    fun setRotateDegree(degree: Float) {
        mTouchPictureModel?.let {
            it.rotateDegree = degree
            Log.d("JigsawView", "rotateDegree: ${it.rotateDegree}")
        }

        invalidate()
    }

    /**
     * 绘制选中的图片的虚影
     */
    private fun drawPictureShadow(canvas: Canvas?) {
        if (!changePicMode) {
            return
        }
        mTouchPictureModel?.let {
            if (!it.isSelected || !isNeedDrawShadow) {
                return
            }

            canvas?.save()

            val scaleX = it.scaleX
            val scaleY = it.scaleY

            // Log.d("JigsawView", "scaleX:$scale")
            val bitmap = it.bitmapPicture

            val hollowModel = it.hollowModel
            val hollowX = hollowModel.hollowX
            val hollowY = hollowModel.hollowY
            val hollowWidth = hollowModel.width
            val hollowHeight = hollowModel.height

            canvas?.translate(hollowX.toFloat(), hollowY.toFloat())
            //图片的中点位置以边框区域中点为标准。根据图片大小以及图片中心点边框区域中点的偏移距离和算出缩放前图片平移后左上角坐标
            val pictureX = hollowWidth / 2 - bitmap.width / 2 + it.xToHollowCenter
            val pictureY = hollowHeight / 2 - bitmap.height / 2 + it.yToHollowCenter

            mMatrix.postTranslate(pictureX.toFloat(), pictureY.toFloat())
            //scalePicWithGap(hollowWidth, it, hollowHeight)
            mMatrix.postScale(scaleX, scaleY, (hollowWidth / 2 + it.xToHollowCenter).toFloat(), (hollowHeight / 2 + it.yToHollowCenter).toFloat())
            mMatrix.postRotate(it.rotateDegree, (hollowWidth / 2 + it.xToHollowCenter).toFloat(), (hollowHeight / 2 + it.yToHollowCenter).toFloat())

            canvas?.drawBitmap(bitmap, mMatrix, mPictureHalfAlphaPaint)
            canvas?.restore()
        }
        mMatrix.reset()
    }

    private fun drawHollow(canvas: Canvas, hollowX: Int, hollowY: Int, rect: RectF, selected: Boolean) {
        if (selected) {
            canvas.drawRoundRect(rect, hollowRoundRadius, hollowRoundRadius, mHollowSelectPaint)
        } else {
            canvas.drawRoundRect(rect, hollowRoundRadius, hollowRoundRadius, mHollowPaint)
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
                downTime = System.currentTimeMillis()

                mLastX = event.x
                mLastY = event.y

                mDownX = event.x
                mDownY = event.y

                Log.d("JigsawView", "mLastX:$mLastX")
                Log.d("JigsawView", "mLastY:$mLastY")

                val tempModel = getTouchPicModel(event)
                //长按选中
                longClickHandler.postDelayed({
                    mTouchPictureModel = tempModel
                    selectPictureModel()
                    changePicMode = true
                    invalidate()
                }, 600L)

                mTouchPictureModel?.refreshIsTouchHollowState(event)

                Log.d("JigsawView", "ACTION_DOWN pointerCount:${event.pointerCount}")
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                //双指模式
                if (event.pointerCount == 2) {

                    isNeedDrawShadow = true
                    doubleTouchMode = true

                    if (mTouchPictureModel != null) {
                        mLastFingerDistance = distanceBetweenFingers(event)

                        Log.d("JigsawView", "ACTION_POINTER_DOWN mLastFingerDistance:$mLastFingerDistance")
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val distanceFromDownPoint = getDisFromDownPoint(event)
                if (distanceFromDownPoint > viewConfig.scaledTouchSlop) {
                    longClickHandler.removeCallbacksAndMessages(null)
                }
                isNeedDrawShadow = true
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

                            if (changePicMode) {
                                willChangeModel = getTouchPicModel(event)
                            }

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

                            val tempScaleX = scaleRatioDelta * it.scaleX
                            val tempScaleY = scaleRatioDelta * it.scaleY

                            //对缩放比做限制
                            if (Math.abs(tempScaleX) < 3 || Math.abs(tempScaleX) > 0.5) {
                                it.scaleX = tempScaleX
                                it.scaleY = tempScaleY

                                invalidate()
                                mLastFingerDistance = fingerDistance

                                Log.d("JigsawView", "mLastFingerDistance:$mLastFingerDistance")
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longClickHandler.removeCallbacksAndMessages(null)

                //交换图片
                if (changePicMode && mTouchPictureModel != null && willChangeModel != null && mTouchPictureModel != willChangeModel) {
                    val tempBitmap = mTouchPictureModel!!.bitmapPicture
                    mTouchPictureModel!!.bitmapPicture = willChangeModel!!.bitmapPicture
                    willChangeModel!!.bitmapPicture = tempBitmap
                    mTouchPictureModel!!.refreshStateWhenChangePic()
                    willChangeModel!!.refreshStateWhenChangePic()
                }

                changePicMode = false
                willChangeModel = null

                val distanceFromDownPoint = getDisFromDownPoint(event)
                if (distanceFromDownPoint < viewConfig.scaledTouchSlop) {
                    //选中状态
                    mTouchPictureModel = getTouchPicModel(event)
                    selectPictureModel()
                    invalidate()
                    isNeedDrawShadow = false
                    return true
                }

                isNeedDrawShadow = false

                Log.d("JigsawView", "MotionEvent: ${event.actionMasked}")

                if (doubleTouchMode) {
                    //缩放图片导致的边框内空白部分动画放大图片填充
                    mTouchPictureModel?.let {
                        it.backToCenterCropState(it, false)
                    }

                } else {
                    mTouchPictureModel?.let {
                        if (!it.isTouchHollow) {
                            //不是拖动边框，而是拖动图片则动画填充
                            it.translatePictureCropHollowByAnimationIfNeed()
                        } else {
                            //拖动边框如果此时有空白则centercrop填充
                            postDelayed({
                                it.backToCenterCropState(it, true)
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
                val x = event.x
                val y = event.y
                Log.d("JigsawView", "getTouchPicModel x:$x y:$y")
                for (picModel in mPictureModelList) {

                    if (mIsRegular) {
                        val hollowX = picModel.hollowModel.hollowX
                        val hollowY = picModel.hollowModel.hollowY
                        val hollowWidth = picModel.hollowModel.width
                        val hollowHeight = picModel.hollowModel.height

                        val rect = RectF(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight)
                        //点在矩形区域中
                        if (rect.contains(x, y)) {
                            return picModel
                        }
                    } else {
                        //不规则图形通过region判断区域
                        val path = picModel.hollowModel.path
                        val re = Region()
                        val r = RectF()
                        //计算控制点的边界
                        path?.let {
                            it.computeBounds(r, true)
                            //设置区域路径和剪辑描述的区域
                            re.setPath(it, Region(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt()))
                            if (re.contains(x.toInt(), y.toInt())) {
                                return picModel
                            }
                        }
                    }

                }
            }
            2 -> {
                val x0 = event.getX(0)
                val y0 = event.getY(0)
                val x1 = event.getX(1)
                val y1 = event.getY(1)
                for (picModel in mPictureModelList) {
                    val hollowX = picModel.hollowModel.hollowX
                    val hollowY = picModel.hollowModel.hollowY
                    val hollowWidth = picModel.hollowModel.width
                    val hollowHeight = picModel.hollowModel.height

                    val rect = RectF(hollowX, hollowY, hollowX + hollowWidth, hollowY + hollowHeight)
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

    class DelayHandler : Handler() {

    }

}