package com.recipesaver.app.data.remote

import com.recipesaver.app.data.remote.dto.ApiData
import com.recipesaver.app.data.remote.dto.RecipeDto
import com.recipesaver.app.data.remote.dto.RecipeImageDto
import com.recipesaver.app.data.remote.dto.RecipeRequestDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Retrofit interface for the Recipe Saver backend (see the sibling `api/` Laravel project and
 * architecture.md §12.4). Every call is authenticated by the `X-API-Key` header added by
 * [ApiKeyInterceptor]. All bodies are suspend calls so they run off the main thread.
 */
interface RecipeApiService {
    @GET("recipes")
    suspend fun listRecipes(): ApiData<List<RecipeDto>>

    @GET("recipes/{id}")
    suspend fun getRecipe(
        @Path("id") id: Long,
    ): ApiData<RecipeDto>

    @POST("recipes")
    suspend fun createRecipe(
        @Body body: RecipeRequestDto,
    ): ApiData<RecipeDto>

    @PUT("recipes/{id}")
    suspend fun updateRecipe(
        @Path("id") id: Long,
        @Body body: RecipeRequestDto,
    ): ApiData<RecipeDto>

    @DELETE("recipes/{id}")
    suspend fun deleteRecipe(
        @Path("id") id: Long,
    )

    @Multipart
    @POST("recipes/{id}/cover")
    suspend fun uploadCover(
        @Path("id") id: Long,
        @Part image: MultipartBody.Part,
    ): ApiData<RecipeDto>

    @DELETE("recipes/{id}/cover")
    suspend fun deleteCover(
        @Path("id") id: Long,
    ): ApiData<RecipeDto>

    @Multipart
    @POST("recipes/{recipeId}/images")
    suspend fun addImage(
        @Path("recipeId") recipeId: Long,
        @Part image: MultipartBody.Part,
    ): ApiData<RecipeImageDto>

    @DELETE("recipes/{recipeId}/images/{imageId}")
    suspend fun deleteImage(
        @Path("recipeId") recipeId: Long,
        @Path("imageId") imageId: Long,
    )
}
