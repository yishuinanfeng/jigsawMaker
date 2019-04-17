package calculatorapp.free.quick.com.jigsawsample

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout


class MainActivity : AppCompatActivity() {
    private var isJigsawInit = false
    private lateinit var jigsawView: JigsawView
    private lateinit var mTemplateInfoProvider:TemplateInfoProvider

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        //显示区的宽高
        addJigsaw(R.raw.layout2)
    }

    private fun addJigsaw(jsonResId: Int) {
        val containerWidth = flContainer.width
        val containerHeight = flContainer.height
        val heightWidthRatio = mTemplateInfoProvider.getJigsawHeightWidthRatio()

        val lp = getCenterJigsawLP(containerWidth, containerHeight, heightWidthRatio)
        if (!isJigsawInit) {
            initJigsaw(lp, jsonResId)
            isJigsawInit = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mTemplateInfoProvider = TemplateInfoProvider(this, R.raw.layout2)
    }


    /**
     *
     */
    private fun getCenterJigsawLP(width: Int, height: Int, JigsawHeightWidthRatio: Float): FrameLayout.LayoutParams {
        val containerHeightWidthRatio = height / width
        return if (containerHeightWidthRatio < JigsawHeightWidthRatio) {
            val resultWidth = height / JigsawHeightWidthRatio
            FrameLayout.LayoutParams(resultWidth.toInt(), height)
        } else {
            val resultHeight = width * JigsawHeightWidthRatio
            FrameLayout.LayoutParams(width, resultHeight.toInt())
        }

    }

    private fun initJigsaw(lp: FrameLayout.LayoutParams, jsonResId: Int) {
        val bitmap1 = BitmapFactory.decodeResource(resources, R.drawable.aa)
        val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.bb)
        val bitmaList = mutableListOf<Bitmap>()
        bitmaList.add(bitmap1)
        bitmaList.add(bitmap2)
        bitmaList.add(bitmap1)
        bitmaList.add(bitmap2)
        val jigsawModelList = mTemplateInfoProvider.getPictureModelList( bitmaList, lp.width)
        val isRegular = mTemplateInfoProvider.getIsRegular()
        jigsawView = JigsawView(this, isRegular)
        jigsawView.setFilterManager(ImageFilterManager(this, jigsawModelList))
        lp.gravity = Gravity.CENTER
        jigsawView.layoutParams = lp
        jigsawView.initPictureModelList(jigsawModelList)
        flContainer.addView(jigsawView)

        gap.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d(TAG, "progress: $progress")
                jigsawView.setGap(progress.toFloat() / 100)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        round.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d(TAG, "progress: $progress")
                jigsawView.setHollowRoundRadius(progress.toFloat() / 100)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        degree.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d(TAG, "progress: $progress")
                jigsawView.setRotateDegree(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })



        needOverTurnHorizontal.setOnClickListener {
            jigsawView.overTurnHorizontal()
        }

        needOverTurnVertical.setOnClickListener {
            jigsawView.overTurnVertical()
        }

        filter1.setOnClickListener {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.film)
            jigsawView.addFilterForOnePic(bitmap)
        }

        filter2.setOnClickListener {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.time)
            jigsawView.addFilterForOnePic(bitmap)
        }

        makeBitmap.setOnClickListener {
            val bitmap = createBitmap(jigsawView)
            imgResult.setImageBitmap(bitmap)
            jigsawView.visibility = View.GONE

            //            val bStream = ByteArrayOutputStream()
            //            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream)
            //            val byteArray = bStream.toByteArray()
            //            ResultActivity.gotoActivity(this,byteArray)
        }
    }

    private fun createBitmap(view: View): Bitmap {
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        view.draw(c)
        return bmp
    }

    private fun initPictureList(): MutableList<PictureModel> {
        val jigsawModelList = mutableListOf<PictureModel>()

        val hollowModel1 = HollowModel(0f, 0f, 400f, 500f)
        val hollowModel2 = HollowModel(0f, 500f, 400f, 500f)
        val hollowModel3 = HollowModel(400f, 0f, 400f, 500f)
        val hollowModel4 = HollowModel(400f, 500f, 400f, 500f)
        val hollowModel5 = HollowModel(800f, 0f, 400f, 500f)
        val hollowModel6 = HollowModel(800f, 500f, 400f, 500f)

        val bitmap1 = BitmapFactory.decodeResource(resources, R.drawable.aa)
        val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.bb)

        val pictureModel1 = PictureModel(bitmap1, hollowModel1)
        val pictureModel2 = PictureModel(bitmap2, hollowModel2)
        val pictureModel3 = PictureModel(bitmap1, hollowModel3)
        val pictureModel4 = PictureModel(bitmap2, hollowModel4)
        val pictureModel5 = PictureModel(bitmap1, hollowModel5)
        val pictureModel6 = PictureModel(bitmap2, hollowModel6)
        jigsawModelList.add(pictureModel1)
        jigsawModelList.add(pictureModel2)
        jigsawModelList.add(pictureModel3)
        jigsawModelList.add(pictureModel4)
        jigsawModelList.add(pictureModel5)
        jigsawModelList.add(pictureModel6)

//        addEffectPicForModel1(pictureModel2, pictureModel4, pictureModel6, pictureModel3, pictureModel5, pictureModel1)
//        addEffectPicForModel2(pictureModel1, pictureModel3, pictureModel5, pictureModel4, pictureModel6, pictureModel2)
//        addEffectPicForModel3(pictureModel2, pictureModel4, pictureModel6, pictureModel1, pictureModel5, pictureModel3)

        return jigsawModelList
    }

    private fun addEffectPicForModel3(pictureModel1: PictureModel, pictureModel2: PictureModel, pictureModel3: PictureModel, pictureModel4: PictureModel, pictureModel5: PictureModel, sourceModel: PictureModel) {
        val list = mutableListOf<PictureModel>()
        list.add(pictureModel1)
        list.add(pictureModel2)
        list.add(pictureModel3)
        val list1 = mutableListOf<PictureModel>()
        list1.add(pictureModel4)
        list1.add(pictureModel5)
        val modelArray = SparseArray<List<PictureModel>>()
        modelArray.put(HollowModel.TOP, list)
        modelArray.put(HollowModel.BOTTOM, list1)
        sourceModel.addEffectPictureModel(modelArray, HollowModel.BOTTOM)
        val list2 = mutableListOf<PictureModel>()
        list2.add(pictureModel5)
        list2.add(pictureModel3)
        val modelArray1 = SparseArray<List<PictureModel>>()
        modelArray1.put(HollowModel.LEFT, list2)
        val list3 = mutableListOf<PictureModel>()
        list3.add(pictureModel2)
        modelArray1.put(HollowModel.RIGHT, list3)
        sourceModel.addEffectPictureModel(modelArray1, HollowModel.RIGHT)

        val list4 = mutableListOf<PictureModel>()
        list4.add(pictureModel2)
        val modelArray2 = SparseArray<List<PictureModel>>()
        modelArray2.put(HollowModel.LEFT, list4)
        val list5 = mutableListOf<PictureModel>()
        list5.add(pictureModel1)
        list5.add(pictureModel4)
        modelArray2.put(HollowModel.RIGHT, list5)
        sourceModel.addEffectPictureModel(modelArray2, HollowModel.LEFT)

    }

    private fun addEffectPicForModel1(pictureModel1: PictureModel, pictureModel2: PictureModel, pictureModel3: PictureModel, pictureModel4: PictureModel, pictureModel5: PictureModel, sourceModel: PictureModel) {
        val list = mutableListOf<PictureModel>()
        list.add(pictureModel1)
        list.add(pictureModel2)
        list.add(pictureModel3)
        val list1 = mutableListOf<PictureModel>()
        list1.add(pictureModel4)
        list1.add(pictureModel5)
        val modelArray = SparseArray<List<PictureModel>>()
        modelArray.put(HollowModel.TOP, list)
        modelArray.put(HollowModel.BOTTOM, list1)
        sourceModel.addEffectPictureModel(modelArray, HollowModel.BOTTOM)
        val list2 = mutableListOf<PictureModel>()
        list2.add(pictureModel2)
        list2.add(pictureModel4)
        val modelArray1 = SparseArray<List<PictureModel>>()
        modelArray1.put(HollowModel.LEFT, list2)
        val list3 = mutableListOf<PictureModel>()
        list3.add(pictureModel1)
        modelArray1.put(HollowModel.RIGHT, list3)
        sourceModel.addEffectPictureModel(modelArray1, HollowModel.RIGHT)
    }

    private fun addEffectPicForModel2(pictureModel1: PictureModel, pictureModel3: PictureModel, pictureModel5: PictureModel, pictureModel4: PictureModel, pictureModel6: PictureModel, sourceModel: PictureModel) {
        val list = mutableListOf<PictureModel>()
        list.add(pictureModel1)
        list.add(pictureModel3)
        list.add(pictureModel5)
        val list1 = mutableListOf<PictureModel>()
        list1.add(pictureModel4)
        list1.add(pictureModel6)
        val modelArray = SparseArray<List<PictureModel>>()
        modelArray.put(HollowModel.BOTTOM, list)
        modelArray.put(HollowModel.TOP, list1)
        sourceModel.addEffectPictureModel(modelArray, HollowModel.TOP)
        val list2 = mutableListOf<PictureModel>()
        list2.add(pictureModel3)
        list2.add(pictureModel4)
        val modelArray1 = SparseArray<List<PictureModel>>()
        modelArray1.put(HollowModel.LEFT, list2)
        val list3 = mutableListOf<PictureModel>()
        list3.add(pictureModel1)
        modelArray1.put(HollowModel.RIGHT, list3)
        sourceModel.addEffectPictureModel(modelArray1, HollowModel.RIGHT)
    }
}
