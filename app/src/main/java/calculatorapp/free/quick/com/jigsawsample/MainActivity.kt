package calculatorapp.free.quick.com.jigsawsample

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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

        val hollowModel1 = HollowModel(100, 100, 400, 500)
        val hollowModel2 = HollowModel(400, 700, 500, 500)

        val bitmap1 = BitmapFactory.decodeResource(resources, R.drawable.aa)
        val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.aa)
        val pictureModel1 = PictureModel(bitmap1, hollowModel1)
        val pictureModel2 = PictureModel(bitmap2, hollowModel2)
        jigsawModelList.add(pictureModel1)
        jigsawModelList.add(pictureModel2)
        return jigsawModelList
    }
}
