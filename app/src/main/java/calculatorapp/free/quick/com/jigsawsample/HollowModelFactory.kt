package calculatorapp.free.quick.com.jigsawsample

import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.support.annotation.IntegerRes
import org.json.JSONObject

/**
 * 创建时间： 2019/4/11
 * 作者：yanyinan
 * 功能描述：
 * @param standLength:表示单位长度，因为json中的长度都是以单位长度为1进行表示的
 */
class HollowModelFactory() {

    companion object {

        fun getHollowListByJsonFile(context: Context,@IntegerRes resId:Int, standLength: Int): List<HollowModel> {
            val hollowList = mutableListOf<HollowModel>()
            val jsonString = readFile(context,resId)
            val jsonObject = JSONObject(jsonString)
            val circleArray = jsonObject.optJSONArray("circles")
            if (circleArray != null) {

            } else {
                //非圆形
                val hollowArray = jsonObject.getJSONArray("hollows")
                hollowArray?.let {
                    val hollowPointArray = mutableListOf<Point>()
                    for (i in 0 until hollowArray.length()) {
                        val hollowLocationStr = hollowArray.optJSONArray(i)
                        val size = hollowLocationStr.length()
                        for (j in 0 until size) {
                            val position: String? = hollowLocationStr[j] as? String
                            val positionArrayForOneHollow = position?.split(" ")
                            positionArrayForOneHollow?.forEach { p ->
                                val xAndY = p.split(",")
                                val x = xAndY[0].toFloat() * standLength
                                val y = xAndY[1].toFloat() * standLength
                                val point = Point(x.toInt(), y.toInt())
                                hollowPointArray.add(point)
                            }
                        }

                        val hollowPath = Path()
                        hollowPointArray.forEachIndexed { index, point ->
                            if (index == 0) {
                                hollowPath.moveTo(point.x.toFloat(), point.y.toFloat())
                            } else {
                                hollowPath.lineTo(point.x.toFloat(), point.y.toFloat())
                            }
                        }
                        val pointHollowArray = getPointsHollow(hollowPointArray)

                        //通过hollowLocationStr求出外接矩形，将外接矩形数据写入HollowModel
                        val hollow = HollowModel(pointHollowArray[0], pointHollowArray[1], pointHollowArray[2] - pointHollowArray[0]
                                , pointHollowArray[1] - pointHollowArray[3], hollowPath)
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
                } else if (x > right || right == -1) {
                    right = x
                }

                if (y < top || top == -1) {
                    top = y
                } else if (y > bottom || bottom == -1) {
                    bottom = y
                }
            }
            return arrayOf(left, top, right, bottom)
        }
    }
}