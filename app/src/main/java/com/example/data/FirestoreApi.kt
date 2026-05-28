package com.example.data

import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class FirestoreValue(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val booleanValue: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreDocument(
    val name: String? = null,
    val fields: Map<String, FirestoreValue>? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreListResponse(
    val documents: List<FirestoreDocument>? = null
)

interface FirestoreService {
    @GET("projects/{projectId}/databases/(default)/documents/artifacts/{appId}/public/data/items")
    suspend fun getItems(
        @Path("projectId") projectId: String,
        @Path("appId") appId: String
    ): FirestoreListResponse

    @PATCH("projects/{projectId}/databases/(default)/documents/artifacts/{appId}/public/data/items/{itemId}")
    suspend fun saveItem(
        @Path("projectId") projectId: String,
        @Path("appId") appId: String,
        @Path("itemId") itemId: String,
        @Body document: FirestoreDocument
    ): FirestoreDocument

    @DELETE("projects/{projectId}/databases/(default)/documents/artifacts/{appId}/public/data/items/{itemId}")
    suspend fun deleteItem(
        @Path("projectId") projectId: String,
        @Path("appId") appId: String,
        @Path("itemId") itemId: String
    )

    @GET("projects/{projectId}/databases/(default)/documents/artifacts/{appId}/public/data/settings/config")
    suspend fun getSettings(
        @Path("projectId") projectId: String,
        @Path("appId") appId: String
    ): FirestoreDocument

    @PATCH("projects/{projectId}/databases/(default)/documents/artifacts/{appId}/public/data/settings/config")
    suspend fun saveSettings(
        @Path("projectId") projectId: String,
        @Path("appId") appId: String,
        @Body document: FirestoreDocument
    ): FirestoreDocument
}

object FirestoreApiClient {
    private const val BASE_URL = "https://firestore.googleapis.com/v1/"

    val service: FirestoreService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(FirestoreService::class.java)
    }
}

// Convert FirestoreDocument structures directly to/from our database Entities!
fun FirestoreDocument.toCatalogItem(docId: String): CatalogItem {
    val f = this.fields ?: emptyMap()
    return CatalogItem(
        id = docId.toLongOrNull() ?: 0L,
        title = f["title"]?.stringValue ?: "",
        category = f["category"]?.stringValue ?: "",
        price = f["price"]?.stringValue ?: "",
        image = f["image"]?.stringValue ?: "",
        desc = f["desc"]?.stringValue ?: "",
        message = f["message"]?.stringValue ?: "",
        createdAt = f["createdAt"]?.integerValue?.toLongOrNull() ?: System.currentTimeMillis()
    )
}

fun CatalogItem.toFirestoreDocument(): FirestoreDocument {
    return FirestoreDocument(
        fields = mapOf(
            "title" to FirestoreValue(stringValue = title),
            "category" to FirestoreValue(stringValue = category),
            "price" to FirestoreValue(stringValue = price),
            "image" to FirestoreValue(stringValue = image),
            "desc" to FirestoreValue(stringValue = desc),
            "message" to FirestoreValue(stringValue = message),
            "createdAt" to FirestoreValue(integerValue = createdAt.toString())
        )
    )
}

fun FirestoreDocument.toCatalogSettings(projectId: String): CatalogSettings {
    val f = this.fields ?: emptyMap()
    return CatalogSettings(
        id = 1,
        appName = f["appName"]?.stringValue ?: "Katalog WhatsApp",
        appSubtitle = f["appSubtitle"]?.stringValue ?: "Katalog Resmi WhatsApp Kami",
        whatsappNumber = f["whatsappNumber"]?.stringValue ?: "",
        hashedPin = f["hashedPin"]?.stringValue ?: "",
        firebaseProjectId = projectId,
        isConfigured = true
    )
}

fun CatalogSettings.toFirestoreDocument(): FirestoreDocument {
    return FirestoreDocument(
        fields = mapOf(
            "appName" to FirestoreValue(stringValue = appName),
            "appSubtitle" to FirestoreValue(stringValue = appSubtitle),
            "whatsappNumber" to FirestoreValue(stringValue = whatsappNumber),
            "hashedPin" to FirestoreValue(stringValue = hashedPin)
        )
    )
}
