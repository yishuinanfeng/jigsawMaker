package calculatorapp.free.quick.com.jigsawsample

import android.content.Context
import android.graphics.Bitmap
import android.util.SparseArray
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageLookupFilter

/**
 * 创建时间： 2019/4/17
 * 作者：yanyinan
 * 功能描述：滤镜管理器
 */
class ImageFilterManager(context: Context, filterModelList: List<FilterModel>) {
    private val mContext = context
    /**
     * key为对应的关联图片的实体类的index
     */
    private val mFilterMap = SparseArray<GPUImage>()
    private val mFilterModelList = filterModelList

    fun addFilterForOnePicture(model: FilterModel, filterBitmap: Bitmap) {
        val index = mFilterModelList.indexOf(model)
        var gpuImage = mFilterMap.get(index)
        var isNewGpuImage = false
        if (gpuImage == null) {
            gpuImage = GPUImage(mContext)
            gpuImage.setImage(model.getBitmap())
            isNewGpuImage = true
        }
        gpuImage.setImage(model.getBitmap())
        val filter = GPUImageLookupFilter()
        filter.bitmap = filterBitmap
//        val filterGroup =GPUImageFilterGroup()
//        filterGroup.addFilter(filter)
        gpuImage.setFilter(filter)
        val resultBitmap = gpuImage.bitmapWithFilterApplied
        model.setBitmapWithFilter(resultBitmap)

        if (isNewGpuImage) {
            mFilterMap.put(index, gpuImage)
        }
    }

    fun addFilterForAllPicture(filterBitmap: Bitmap) {
        mFilterModelList.forEach {
            addFilterForOnePicture(it, filterBitmap)
        }
    }

    interface FilterModel {
        fun setBitmapWithFilter(bitmap: Bitmap)

        fun getBitmap(): Bitmap
    }

}