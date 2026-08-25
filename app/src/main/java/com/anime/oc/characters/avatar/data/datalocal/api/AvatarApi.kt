package com.anime.oc.characters.avatar.data.datalocal.api

import android.util.Log
import com.anime.oc.characters.avatar.data.model.custom.BodyPartModel
import com.anime.oc.characters.avatar.data.model.custom.ColorModel
import com.anime.oc.characters.avatar.data.model.custom.CustomModel
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

// ── CONFIG ────────────────────────────────────────────────────────────────────

object ApiConfig {
    const val BASE_URL_1   = "https://lvtglobal.tech/"
    const val BASE_URL_2   = "https://lvt-api-tech.io.vn/"
    const val BASE_CONNECT = "public/app/ST279_AnimeOCMaker/"

    var BASE_URL = BASE_URL_1
}
// ── RESPONSE WRAPPER ──────────────────────────────────────────────────────────
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data")    val data: T?          = null,
    @SerializedName("message") val message: String?  = null,
    @SerializedName("code")    val code: Int          = 200
)
// ── API MODEL ─────────────────────────────────────────────────────────────────
data class X10(
    @SerializedName("colorArray") val colorArray: String,
    @SerializedName("parts")      val parts: String,
    @SerializedName("position")   val position: String,
    @SerializedName("quantity")   val quantity: String,
    @SerializedName("level")      val level: String
) {
    val quantityInt: Int get() = quantity.toIntOrNull() ?: 0
    val levelInt: Int   get() = level.toIntOrNull() ?: Int.MAX_VALUE
}
// ── SERVICE ───────────────────────────────────────────────────────────────────
interface AvatarApiService {
    @GET("api/app/ST279_AnimeOCMaker")
    suspend fun getAllData(): Map<String, List<X10>>
}
// ── MAPPER ────────────────────────────────────────────────────────────────────
object ApiTemplateMapper {
    fun map(
        raw: Map<String, List<X10>>,
        base: String,
        conn: String,
        idPrefix: String
    ): List<CustomModel> {
        return raw.entries
            .sortedBy { (_, list) -> list.firstOrNull()?.levelInt ?: Int.MAX_VALUE }
            .map { (key, list) ->
                val bodyParts = list
                    .sortedBy { it.parts.substringBefore("-").toIntOrNull() ?: 999 }
                    .map { x10 ->
                        val parts = x10.parts.split("-")
                        val x = parts.getOrNull(0)?.toIntOrNull() ?: 0  // số TRƯỚC "-"
                        val y = parts.getOrNull(1)?.toIntOrNull() ?: 0  // số SAU "-"
                        val (colors, listThumbPath) = buildColorsAndThumbs(x10, base, conn)
                        BodyPartModel(
                            nav = "${base}${conn}${x10.position}/${x10.parts}/nav.png",
                            listPath = colors,
                            listThumbPath = listThumbPath,
                            position = x,
                            zIndex = y
                        )
                    }
                val minZIndex = bodyParts.minOfOrNull { it.zIndex } ?: Int.MAX_VALUE
                bodyParts.forEach { bp ->
                    bp.listPath.forEach { cm ->
                        if (cm.listPath.isEmpty()) return@forEach  // guard
                        when {
                            bp.zIndex == minZIndex -> {
                                // Body chính: chỉ dice
                                if (cm.listPath.first() != "dice") cm.listPath.add(0, "dice")
                            }
                            else -> {
                                // Nav khác: none + dice
                                if (cm.listPath.first() != "none") {
                                    cm.listPath.add(0, "none")
                                    cm.listPath.add(1, "dice")
                                }
                            }
                        }
                    }
                }

                CustomModel(
                    id = "${idPrefix}_$key",
                    avatar = "${base}${conn}$key/avatar.png",
                    listPath = ArrayList(bodyParts),
                    selections = arrayListOf(),
                    updatedAt = System.currentTimeMillis()
                )
            }
    }

    private fun buildColorsAndThumbs(
        x10: X10,
        base: String,
        conn: String
    ): Pair<ArrayList<ColorModel>, ArrayList<String>> {
        val colors        = arrayListOf<ColorModel>()
        val listThumbPath = arrayListOf<String>()
        val qty           = x10.quantityInt

        if (x10.colorArray.isEmpty()) {
            for (i in 1..qty) {
                listThumbPath.add("${base}${conn}${x10.position}/${x10.parts}/thumb_$i.png")
            }
            val realPaths = (1..qty).map { i ->
                "${base}${conn}${x10.position}/${x10.parts}/$i.png"
            }
            colors.add(ColorModel("", ArrayList(realPaths)))
        } else {
            for (i in 1..qty) {
                listThumbPath.add("${base}${conn}${x10.position}/${x10.parts}/thumb_$i.png")
            }
            x10.colorArray.split(",").forEach { color ->
                val paths = (1..qty).map { i ->
                    "${base}${conn}${x10.position}/${x10.parts}/$color/$i.png"
                }
                colors.add(ColorModel(color, ArrayList(paths)))
            }
        }

        // KHÔNG add "none"/"dice" ở đây — để map() xử lý sau khi có đủ thông tin position
        return colors to listThumbPath
    }
}

// ── RESULT SEALED CLASS ───────────────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T)         : ApiResult<T>()
    data class Error(val message: String)      : ApiResult<Nothing>()
}

// ── REMOTE DATA SOURCE ────────────────────────────────────────────────────────
@Singleton
class RemoteDataSource @Inject constructor(
    private val apiHelper: ApiHelper   // ← đổi sang ApiHelper
) {
    companion object { private const val TAG = "RemoteDataSource" }

    suspend fun fetchTemplates(): ApiResult<List<CustomModel>> = withContext(Dispatchers.IO) {
        fetchSingleTemplates()
    }

    private suspend fun fetchSingleTemplates(): ApiResult<List<CustomModel>> {
        return try {
            val body = apiHelper.api1.getAllData()
            Log.d(TAG, "✅ Single URL1 success, size: ${body.size}")
            ApiResult.Success(ApiTemplateMapper.map(
                body, ApiConfig.BASE_URL_1, ApiConfig.BASE_CONNECT, "online_single"
            ))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Single URL1 failed, trying URL2: ${e.message}")
            try {
                val body = apiHelper.api2.getAllData()
                ApiResult.Success(ApiTemplateMapper.map(
                    body, ApiConfig.BASE_URL_2, ApiConfig.BASE_CONNECT, "online_single"
                ))
            } catch (e2: Exception) {
                ApiResult.Error("Single: ${e2.message ?: "Network error"}")
            }
        }
    }
}
