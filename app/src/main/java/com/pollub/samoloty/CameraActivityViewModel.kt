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

    //obiekty przechowujące dane
    private val renderData: MutableLiveData<List<RenderData>> = MutableLiveData()
    private val planesData: MutableLiveData<List<Plane>> = MutableLiveData()

    //funkcje pozwalające na obserwowanie z głównego wątka zmian w danych
    fun getPlanes(): LiveData<List<Plane>> = planesData
    fun getRenderData(): LiveData<List<RenderData>> = renderData

    //funkcja wczytująca potrzebne dane w oddzielnym wątku
    fun loadData(assets: AssetManager) {

        viewModelScope.launch(Dispatchers.IO) {

            //pobranie z bazy danych obiektów zawierających dane o samolotach
            val planes = Repository.getAll()

            Log.d("dbCount", planes.size.toString())

            //powiadomienie o wczytaniu samolotów (główny wątek)
            withContext(Dispatchers.Main) { planesData.value = planes }

            val data = ArrayList<RenderData>()

            //równoległe wczytywanie modeli i tekstur
            Collections.synchronizedList(planes).parallelStream().forEach { plane ->

                val model = ObjModel()
                try {
                    val modelPath = "models/" + plane.modelFilename
                    model.loadModel(assets, modelPath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@forEach
                }

                val texturePath = "textures/" + plane.textureFilename
                val texture = Texture.loadTextureFromApk(texturePath, assets)
                data.add(RenderData(plane.targetName, model, texture!!))
            }

            //powiadomienie o wczytaniu danych niezbędnych do wyświetlania samolotów
            //(główny wątek)
            withContext(Dispatchers.Main) {
                renderData.value = data
            }
        }
    }
}