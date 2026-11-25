package com.sor999.dalmoi.platform.tracking

import com.sor999.dalmoi.data.local.AuthSession
import com.sor999.dalmoi.data.remote.ApiClient
import com.sor999.dalmoi.data.remote.apiJson
import com.sor999.dalmoi.domain.model.GeoPoint
import io.ktor.http.HttpMethod
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

@Serializable
internal data class GpsSample(
    val eventId: String,
    val sequence: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val recordedAt: String,
)

@Serializable
internal data class RunnerPosition(
    val memberId: Long,
    val latitude: Double,
    val longitude: Double,
    val isStale: Boolean,
) {
    val point get() = GeoPoint(latitude, longitude)
}

@Serializable
private data class PositionsDto(val locations: List<RunnerPosition>)

@Serializable
private data class GpsResult(val accepted: Boolean, val processingStatus: String)

internal class RunLocationApi(private val api: ApiClient, private val auth: AuthSession, private val baseUrl: String) {
    private val sockets = OkHttpClient.Builder().pingInterval(15, TimeUnit.SECONDS).followRedirects(false).build()
    val signedIn get() = auth.signedIn

    suspend fun submit(sessionId: Long, sample: GpsSample): Boolean {
        val data = requireNotNull(
            api.call("sessions/$sessionId/locations", HttpMethod.Post, apiJson.encodeToJsonElement(sample)),
        )
        val result = apiJson.decodeFromJsonElement<GpsResult>(data)
        return result.accepted && result.processingStatus == "VALID"
    }

    suspend fun latest(sessionId: Long): List<RunnerPosition> {
        val data = requireNotNull(api.call("sessions/$sessionId/locations/latest"))
        return apiJson.decodeFromJsonElement<PositionsDto>(data).locations
    }

    // The broker invalidates the snapshot; REST remains authoritative for removals and stale markers.
    fun changes(sessionId: Long) = callbackFlow {
        val token = auth.current?.accessToken ?: throw IOException("Missing location subscription token")
        val url = baseUrl.toHttpUrl().newBuilder().encodedPath("/ws").query(null).fragment(null).build()
        val request = Request.Builder().url(url).header("Sec-WebSocket-Protocol", "v12.stomp").build()
        val socket = sockets.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val headers = "accept-version:1.2\nhost:${url.host}\nAuthorization:Bearer $token"
                    webSocket.send("CONNECT\n$headers\n\n\u0000")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    for (frame in text.split('\u0000')) {
                        when (frame.trimStart().lineSequence().first().trim()) {
                            "CONNECTED" -> {
                                val topic = "/topic/sessions/$sessionId"
                                for (name in listOf("locations", "status")) {
                                    webSocket.send("SUBSCRIBE\nid:$name\ndestination:$topic/$name\n\n\u0000")
                                }
                                trySend(Unit)
                            }

                            "MESSAGE" -> trySend(Unit)

                            "ERROR" -> close(IOException("Location subscription rejected"))
                        }
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    close()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    close(t)
                }
            },
        )
        awaitClose { socket.cancel() }
    }
}
