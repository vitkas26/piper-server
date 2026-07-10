package kg.nurtelecom.piper

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kg.nurtelecom.piper.routes.ErrorResponse
import kg.nurtelecom.piper.routes.ttsRoutes
import kg.nurtelecom.piper.tts.PersistentWorkerTtsService
import kg.nurtelecom.piper.tts.TtsService
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8002, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true })
    }
    install(CallLogging) {
        level = Level.INFO
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Необработанная ошибка", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Внутренняя ошибка сервера"))
        }
    }

    val ttsService: TtsService = PersistentWorkerTtsService()

    routing {
        get("/health") {
            call.respondText("ok")
        }
        ttsRoutes(ttsService)
    }
}
