package calculatorapp.free.quick.com.jigsawsample

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.SparseArray
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val jigsawModelList = initPictureList()
        val jigsawView = JigsawView(this, jigsawModelList)
        flContainer.addView(jigsawView)
    }

    fun initPictureList(): MutableList<PictureModel> {
        val jigsawModelList = mutableListOf<PictureModel>()

        val hollowModel1 = HollowModel(0, 0, 400, 500)
        val hollowModel2 = HollowModel(0, 500, 400, 500)
        val hollowModel3 = HollowModel(400, 0, 400, 500)
        val hollowModel4 = HollowModel(400, 500, 400, 500)
        val hollowModel5 = HollowModel(800, 0, 400, 500)
        val hollowModel6 = HollowModel(800, 500, 400, 500)

        val bitmap1 = BitmapFactory.decodeResource(resources, R.drawable.aa)
        val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.aa)

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
        val list = mutableListOf<PictureModel>()

        list.add(pictureModel1)
        list.add(pictureModel3)
        list.add(pictureModel5)

        val list1 = mutableListOf<PictureModel>()
        list1.add(pictureModel4)
        list1.add(pictureModel6)

        val modelArray = SparseArray<List<PictureModel>>()
        modelArray.put(HollowModel.BOTTOM,list)
        modelArray.put(HollowModel.TOP,list1)
        pictureModel2.addEffectPictureModel(modelArray,HollowModel.TOP)


        return jigsawModelList
    }
}
