package com.anime.oc.characters.avatar.data.repository

import com.anime.oc.characters.avatar.data.datalocal.api.CatalogueApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository chịu trách nhiệm gọi API liên quan đến CustomModel (characters)
 */
@Singleton
class ApiRepository @Inject constructor(
    private val catalogueApi: CatalogueApi
) {
    suspend fun getCatalogue() = withContext(Dispatchers.IO) {
        catalogueApi.getData()
    }
}
