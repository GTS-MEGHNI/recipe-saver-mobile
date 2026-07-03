package com.recipesaver.app.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.recipesaver.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

/**
 * Builds the singleton [RecipeApiService] from the build-time API base URL and key. Kept as a plain
 * object rather than a DI framework, matching the app's lightweight manual wiring in MainActivity.
 */
object NetworkModule {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    fun createApiService(): RecipeApiService {
        val logging =
            HttpLoggingInterceptor().apply {
                level =
                    if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
            }

        val client =
            OkHttpClient.Builder()
                .addInterceptor(ApiKeyInterceptor(BuildConfig.API_KEY))
                .addInterceptor(logging)
                .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RecipeApiService::class.java)
    }
}
