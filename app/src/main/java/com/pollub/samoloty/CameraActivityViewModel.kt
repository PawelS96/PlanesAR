package com.pollub.samoloty

import android.content.res.AssetManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pollub.samoloty.database.Plane
import com.pollub.samoloty.database.Repository
import com.pollub.samoloty.render.ObjModel
import com.pollub.samoloty.render.RenderData
import com.pollub.samoloty.render.Texture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class CameraActivityViewModel : ViewModel() {

    private val renderData: MutableLiveData<List<RenderData>> = MutableLiveData()
    private val planesData: MutableLiveData<List<Plane>> = MutableLiveData()

    fun getPlanes(): LiveData<List<Plane>> = planesData
    fun getRenderData(): LiveData<List<RenderData>> = renderData

    fun loadData(assets: AssetManager) {

        viewModelScope.launch(Dispatchers.IO) {

            val planes = Repository.getAll()

            withContext(Dispatchers.Main) { planesData.value = planes }

            val data = ArrayList<RenderData>()

            Collections.synchronizedList(planes).parallelStream().forEach { plane ->

                Log.d("plane", plane.toString())

                val model = ObjModel()
                try {
                    model.loadModel(assets, plane.modelFilepath)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val texture = Texture.loadTextureFromApk(plane.textureFilepath, assets)
                data.add(RenderData(plane.targetName, model, texture!!))
            }

            withContext(Dispatchers.Main) {
                renderData.value = data
            }

        }
    }
}