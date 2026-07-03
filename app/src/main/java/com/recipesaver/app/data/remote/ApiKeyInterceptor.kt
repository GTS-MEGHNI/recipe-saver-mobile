package com.recipesaver.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the static `X-API-Key` header to every request. This is the app's only authentication (no
 * user accounts) — see architecture.md §12.2. The key is injected at build time via [BuildConfig],
 * not hardcoded here.
 */
class ApiKeyInterceptor(
    private val apiKey: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain.request().newBuilder()
                .header("X-API-Key", apiKey)
                .header("Accept", "application/json")
                .build()
        return chain.proceed(request)
    }
}
