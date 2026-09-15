package com.vetdose.app.data.remote

import com.vetdose.app.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites every request's scheme/host/port to the server address currently
 * stored in [SettingsRepository], so the user can change it from the
 * Settings screen without the app needing to rebuild Retrofit. Retrofit
 * itself is still given a fixed placeholder base URL at construction time
 * (a non-empty one is mandatory), which this interceptor always overrides.
 */
class BaseUrlInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val configuredUrl = runBlocking { settingsRepository.serverBaseUrlFlow.first() }
        val configured = configuredUrl.toHttpUrlOrNull()

        val request = if (configured != null) {
            val newUrl = original.url.newBuilder()
                .scheme(configured.scheme)
                .host(configured.host)
                .port(configured.port)
                .build()
            original.newBuilder().url(newUrl).build()
        } else {
            original
        }

        return chain.proceed(request)
    }
}
