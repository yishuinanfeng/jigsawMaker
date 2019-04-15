package calculatorapp.free.quick.com.jigsawsample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Point
import android.support.annotation.RawRes
import android.util.SparseArray
import org.json.JSONObject

/**
 * 创建时间： 2019/4/9
 * 作者：yanyinan
 * 功能描述：
 */
class PictureModelFactory {

    companion object {
        private const val IS_REGULAR_JSON_KEY = "isRegular"
        private const val HOLLOW_JSON_KEY = "hollows"
        private const val CIRCLE_JSON_KEY = "circles"
        private const val CONTROL_JSON_KEY = "controls"

        fun getPictureModelList(context: Context, bitmapList: List<Bitmap>, @RawRes resId: Int, standLength: Int): List<PictureModel> {
            val pictureList = mutableListOf<PictureModel>()
            val jsonString = readFile(context, resId)
            val jsonObject = JSONObject(jsonString)
            val isRegular = jsonObject.optBoolean(IS_REGULAR_JSON_KEY)
            val hollowList = getHollowListByJsonFile(standLength, jsonObject, isRegular)
            hollowList.forEachIndexed { index, hollowModel ->
                val pictureModel = PictureModel(bitmapList[index], hollowModel)
                pictureList.add(pictureModel)
            }
            if (isRegular) {
                handleEffectPicModel(jsonObject, pictureList)
            }
            return pictureList
        }

        private fun handleEffectPicModel(jsonObject: JSONObject, pictureList: MutableList<PictureModel>) {
            val controls = jsonObject.optJSONArray(CONTROL_JSON_KEY)
            controls?.let {
                for (i in 0 until controls.length()) {
                    val leftMap = SparseArray<List<PictureModel>>()
                    val topMap = SparseArray<List<PictureModel>>()
                    val rightMap = SparseArray<List<PictureModel>>()
                    val bottomMap = SparseArray<List<PictureModel>>()

                    val hollowLocationStr = controls[i] as? String
                    //当前的PictureModel
                    val currentPictureModel = pictureList[i]

                    val effectHollow = hollowLocationStr?.split(" ")

                    val picListLeftRight = mutableListOf<PictureModel>()
                    val picListLeftLeft = mutableListOf<PictureModel>()
                    val picListRightLeft = mutableListOf<PictureModel>()
                    val picListRightRight = mutableListOf<PictureModel>()
                    val picListTopBottom = mutableListOf<PictureModel>()
                    val picListTopTop = mutableListOf<PictureModel>()
                    val picListBottomTop = mutableListOf<PictureModel>()
                    val picListBottomBottom = mutableListOf<PictureModel>()

                    effectHollow?.forEach { effect ->
                        val effectFactor = effect.split(",")
                        //当前Hollow拖动的边
                        val currentDirection = effectFactor[0].toInt()
                        //联动到的Hollow
                        val targetHollowIndex = effectFactor[1].toInt()
                        //联动到的Hollow被联动的边
                        val targetHollowDirection = effectFactor[2].toInt()

                        //被联动的PictureModel
                        val targetPictureModel = pictureList[targetHollowIndex]

                        when (currentDirection) {

                            HollowModel.LEFT -> {
                                when (targetHollowDirection) {
                                    HollowModel.LEFT -> {
                                        picListLeftLeft.add(targetPictureModel)
                                    }

                                    HollowModel.RIGHT -> {
                                        picListLeftRight.add(targetPictureModel)
                                    }

                                }
                            }

                            HollowModel.TOP -> {
                                when (targetHollowDirection) {
                                    HollowModel.TOP -> {
                                        picListTopTop.add(targetPictureModel)
                                    }

                                    HollowModel.BOTTOM -> {
                                        picListTopBottom.add(targetPictureModel)
                                    }

                                }
                            }

                            HollowModel.RIGHT -> {
                                when (targetHollowDirection) {
                                    HollowModel.LEFT -> {
                                        picListRightLeft.add(targetPictureModel)
                                    }

                                    HollowModel.RIGHT -> {
                                        picListRightRight.add(targetPictureModel)
                                    }

                                }
                            }

                            HollowModel.BOTTOM -> {
                                when (targetHollowDirection) {
                                    HollowModel.TOP -> {
                                        picListBottomTop.add(targetPictureModel)
                                    }

                                    HollowModel.BOTTOM -> {
                                        picListBottomBottom.add(targetPictureModel)
                                    }

                                }
                            }
                        }

                    }

                    if (picListLeftLeft.size > 0) {
                        leftMap.put(HollowModel.LEFT, picListLeftLeft)
                    }
                    if (picListLeftRight.size > 0) {
                        leftMap.put(HollowModel.RIGHT, picListLeftRight)
                    }
                    currentPictureModel.addEffectPictureModel(leftMap, HollowModel.LEFT)

                    if (picListTopTop.size > 0) {
                        topMap.put(HollowModel.TOP, picListTopTop)
                    }
                    if (picListTopBottom.size > 0) {
                        topMap.put(HollowModel.BOTTOM, picListTopBottom)
                    }
                    currentPictureModel.addEffectPictureModel(topMap, HollowModel.TOP)


                    if (picListRightLeft.size > 0) {
                        rightMap.put(HollowModel.LEFT, picListRightLeft)
                    }
                    if (picListRightRight.size > 0) {
                        rightMap.put(HollowModel.RIGHT, picListRightRight)
                    }
                    currentPictureModel.addEffectPictureModel(rightMap, HollowModel.RIGHT)


                    if (picListBottomTop.size > 0) {
                        bottomMap.put(HollowModel.TOP, picListBottomTop)
                    }
                    if (picListBottomBottom.size > 0) {
                        bottomMap.put(HollowModel.BOTTOM, picListBottomBottom)
                    }
                    currentPictureModel.addEffectPictureModel(bottomMap, HollowModel.BOTTOM)

                }

            }
        }

        private fun getHollowListByJsonFile(standLength: Int, jsonObject: JSONObject, regular: Boolean): List<HollowModel> {
            val hollowList = mutableListOf<HollowModel>()

            val circleArray = jsonObject.optJSONArray(CIRCLE_JSON_KEY)
            if (circleArray != null) {

            } else {
                //非圆形
                val hollowArray = jsonObject.optJSONArray(HOLLOW_JSON_KEY)
                val hollowPointArray = mutableListOf<Point>()
                hollowArray?.let {
                    for (i in 0 until hollowArray.length()) {
                        hollowPointArray.clear()

                        val hollowLocationStr = hollowArray[i] as? String
                        val positionArrayForOneHollow = hollowLocationStr?.split(" ")

                        positionArrayForOneHollow?.forEach { p ->
                            val xAndY = p.split(",")
                            val x = xAndY[0].toFloat() * standLength
                            val y = xAndY[1].toFloat() * standLength
                            val point = Point(x.toInt(), y.toInt())
                            hollowPointArray.add(point)
                        }

                        val hollowPath = Path()
                        if (!regular) {
                            hollowPointArray.forEachIndexed { index, point ->
                                if (index == 0) {
                                    hollowPath.moveTo(point.x.toFloat(), point.y.toFloat())
                                } else {
                                    hollowPath.lineTo(point.x.toFloat(), point.y.toFloat())
                                }
                            }
                            //闭合
                            hollowPath.close()
                        }

                        val pointHollowArray = getPointsHollow(hollowPointArray)

                        //通过hollowLocationStr求出外接矩形，将外接矩形数据写入HollowModel
                        val hollow = if (regular) {
                            HollowModel(pointHollowArray[0], pointHollowArray[1], pointHollowArray[2] - pointHollowArray[0]
                                    , pointHollowArray[3] - pointHollowArray[1])
                        } else {
                            HollowModel(pointHollowArray[0], pointHollowArray[1], pointHollowArray[2] - pointHollowArray[0]
                                    , pointHollowArray[3] - pointHollowArray[1], hollowPath)
                        }

                        hollowList.add(hollow)
                    }
                }
            }

            return hollowList
        }

        /**
         * 拿到各个顶点对应的图形的外接矩形的坐标点
         */
        private fun getPointsHollow(hollowPointArray: MutableList<Point>): Array<Int> {
            var left: Int = -1
            var top: Int = -1
            var right: Int = -1
            var bottom: Int = -1
            hollowPointArray.forEach { point ->
                val x = point.x
                val y = point.y
                if (x < left || left == -1) {
                    left = x
                }

                if (x > right || right == -1) {
                    right = x
                }

                if (y < top || top == -1) {
                    top = y
                }

                if (y > bottom || bottom == -1) {
                    bottom = y
                }
            }
            return arrayOf(left, top, right, bottom)
        }
    }

}