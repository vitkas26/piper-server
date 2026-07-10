package kg.nurtelecom.piper.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kg.nurtelecom.piper.config.ServerConfig
import kg.nurtelecom.piper.tts.TtsService
import kotlinx.serialization.Serializable

@Serializable
data class TtsRequest(
    val text: String,
    val language: String? = null,
    val speed: Double? = null,
)

@Serializable
data class ErrorResponse(val error: String)

fun Route.ttsRoutes(ttsService: TtsService) {
    post("/tts/synthesize") {
        val request = call.receive<TtsRequest>()

        if (request.text.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Пустой текст для синтеза"))
            return@post
        }

        val language = request.language ?: ServerConfig.defaultLanguage
        if (language !in ServerConfig.voiceModelPaths) {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Неизвестный язык '$language'. Доступные: ${ServerConfig.voiceModelPaths.keys}")
            )
            return@post
        }

        val speed = request.speed ?: 1.0
        if (speed <= 0.0) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("speed должен быть > 0"))
            return@post
        }

        try {
            val wavFile = ttsService.synthesize(request.text, language, speed)
            try {
                call.respondBytes(bytes = wavFile.readBytes(), contentType = ContentType("audio", "wav"))
            } finally {
                wavFile.delete()
            }
        } catch (e: Exception) {
            call.application.log.error("TTS ошибка", e)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Неизвестная ошибка TTS"))
        }
    }
}
