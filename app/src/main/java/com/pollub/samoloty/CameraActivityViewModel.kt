package com.pollub.samoloty

import android.content.res.AssetManager
import android.os.Build
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
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

class CameraActivityViewModel : ViewModel() {

    //obiekty przechowujące dane
    private val renderData: MutableLiveData<List<RenderData>> = MutableLiveData()
    private val planesData: MutableLiveData<List<Plane>> = MutableLiveData()
    private val loadedCount: MutableLiveData<List<Int>> = MutableLiveData()

    //funkcje pozwalające na obserwowanie z głównego wątka zmian w danych
    fun getPlanes(): LiveData<List<Plane>> = planesData

    fun getLoadProgress(): LiveData<List<Int>> = loadedCount

    fun getRenderData(): LiveData<List<RenderData>> = renderData

    //funkcja wczytująca potrzebne dane w oddzielnym wątku
    fun loadData(assets: AssetManager) {

        viewModelScope.launch(Dispatchers.IO) {

            //pobranie z bazy danych obiektów zawierających dane o samolotach
            val planes = Repository.getAll()

            //powiadomienie o wczytaniu samolotów (główny wątek)
            withContext(Dispatchers.Main) {
                planesData.value = planes
                loadedCount.value = listOf(1, planes.size)
            }

            val data = ArrayList<RenderData>()
            val textures = mutableMapOf<String, Texture>()

            suspend fun loadPlane(plane: Plane) {
                val model = ObjModel()
                try {
                    val modelPath = "models/" + plane.modelFilename
                    model.loadModel(assets, modelPath)
                } catch (e: Exception) {
                    Log.e("modelError", e.message)
                    return
                }

                val texturePath = "textures/" + plane.textureFilename
                val texture: Texture = textures[texturePath]
                        ?: Texture.loadTextureFromApk(texturePath, assets).also { textures[texturePath] = it }
                data.add(RenderData(plane.targetName, model, texture, plane.scale, plane.rotation))

                withContext(Dispatchers.Main) {
                    loadedCount.value = listOf(min(data.size + 1, planes.size), planes.size)
                }
            }

            val start = System.currentTimeMillis()

            //równoległe wczytywanie modeli i tekstur
            if (Build.VERSION.SDK_INT >= 24) {

                Collections.synchronizedList(planes).parallelStream().forEach { plane ->

                    runBlocking (Dispatchers.IO) {
                        loadPlane(plane)
                    }
                }
            } else {
                planes.forEachParallel { plane -> loadPlane(plane) }
            }

            //powiadomienie o wczytaniu danych niezbędnych do wyświetlania samolotów
            //(główny wątek)
            withContext(Dispatchers.Main) {
                val end = System.currentTimeMillis()
                Log.d("loadTime", (end - start).toString())
                renderData.value = data
            }
        }
    }
}

fun <A> Collection<A>.forEachParallel(f: suspend (A) -> Unit): Unit = runBlocking {
    map { async(Dispatchers.IO) { f(it) } }.forEach { it.await() }
}