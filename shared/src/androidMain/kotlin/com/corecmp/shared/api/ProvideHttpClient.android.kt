package com.corecmp.shared.api

import android.content.Context
import android.os.Looper
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSource
import java.util.concurrent.Executors

import io.ktor.client.plugins.websocket.WebSockets

/**
 * Navigation clears a ViewModel on the main thread. Ktor then closes the OkHttp
 * response inside that cancel, and the SSL socket close throws
 * NetworkOnMainThreadException out of the coroutine completion handler.
 * Closing the body off the main thread keeps that cancel from crashing the app.
 */
private val responseBodyCloseExecutor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "okhttp-body-close").apply { isDaemon = true }
}

private class OffMainCloseResponseBody(
    private val delegate: ResponseBody,
) : ResponseBody() {
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()
    override fun source(): BufferedSource = delegate.source()

    override fun close() {
        val closeOnMain = Looper.myLooper() == Looper.getMainLooper()
        if (!closeOnMain) {
            runCatching { delegate.close() }
            return
        }
        responseBodyCloseExecutor.execute {
            runCatching { delegate.close() }
        }
    }
}

private class OffMainCloseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        return response.newBuilder()
            .body(OffMainCloseResponseBody(body))
            .build()
    }
}

actual fun provideHttpClient(): HttpClient {

    return HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                addInterceptor(OffMainCloseInterceptor())
            }
        }

        install(WebSockets)

        install(ContentNegotiation) {

            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = if (CoreCmpLogger.isDebugEnabled) LogLevel.INFO else LogLevel.NONE
        }
    }
}
lateinit var appContext: Context

fun isAppContextInitialized(): Boolean = ::appContext.isInitialized

fun initCoreCmp(context: Context, name : String) {
    appContext = context
    SETTINGS_NAME = name

    try {
        val pref = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        if (!pref.contains("core_cmp_initialized")) {
            pref.edit().putString("core_cmp_initialized", "true").apply()
        }
    } catch (_: Exception) {}
}


actual fun provideSettings(): Settings {
    if (!::appContext.isInitialized) {
        error("Call CoreCmp.init(context) in Application.onCreate() before starting Koin on Android.")
    }

    val pref = appContext.getSharedPreferences(
        SETTINGS_NAME,
        Context.MODE_PRIVATE
    )

    return SharedPreferencesSettings(pref)
}

actual fun provideSqlDriver(): app.cash.sqldelight.db.SqlDriver {
    if (!::appContext.isInitialized) {
        error("Call CoreCmp.init(context) in Application.onCreate() before using DB on Android.")
    }
    return com.corecmp.shared.db.DatabaseDriverFactory(appContext).createDriver()
}