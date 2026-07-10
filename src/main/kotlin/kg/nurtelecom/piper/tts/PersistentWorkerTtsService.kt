package kg.nurtelecom.piper.tts

import kg.nurtelecom.piper.config.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

/**
 * Piper сам по себе лёгкий (в отличие от Whisper large) — subprocess-на-запрос был бы
 * не так уж плохо. Но держим тот же персистентный паттерн, что и в whisper-server —
 * единообразие между тремя серверами упрощает поддержку, плюс не платим даже
 * небольшую задержку загрузки голоса на каждый запрос.
 */
class PersistentWorkerTtsService : TtsService {

    @Serializable
    private data class WorkerRequest(
        val text: String,
        val output_path: String,
        val length_scale: Double? = null,
    )

    @Serializable
    private data class WorkerResponse(val success: Boolean = false, val error: String? = null)

    private class Worker {
        val mutex = Mutex()
        var process: Process? = null
        var writer: BufferedWriter? = null
        var reader: BufferedReader? = null
    }

    // Создание воркеров сериализуем отдельным мьютексом, а не общим на все языки —
    // иначе синтез на "ru" ждал бы синтез на "en".
    private val creationMutex = Mutex()
    private val workers = mutableMapOf<String, Worker>()

    private suspend fun getWorker(language: String): Worker = creationMutex.withLock {
        workers.getOrPut(language) { Worker() }
    }

    private fun ensureWorkerStarted(worker: Worker, language: String) {
        if (worker.process?.isAlive == true) return

        val voiceModelPath = ServerConfig.voiceModelPaths[language]
            ?: throw TtsInferenceException(
                "Нет голоса для языка '$language'. Доступные: ${ServerConfig.voiceModelPaths.keys}"
            )

        val newProcess = ProcessBuilder(
            ServerConfig.pythonExecutable,
            ServerConfig.workerScript,
            voiceModelPath
        ).redirectErrorStream(false).start()

        worker.process = newProcess
        worker.writer = BufferedWriter(OutputStreamWriter(newProcess.outputStream))
        worker.reader = BufferedReader(InputStreamReader(newProcess.inputStream))

        Thread {
            newProcess.errorStream.bufferedReader().forEachLine { line ->
                println("[piper_worker:$language] $line")
            }
        }.apply { isDaemon = true }.start()
    }

    override suspend fun synthesize(text: String, language: String, speed: Double): File {
        val worker = getWorker(language)
        return worker.mutex.withLock {
            withContext(Dispatchers.IO) {
                ensureWorkerStarted(worker, language)

                val w = worker.writer ?: throw TtsInferenceException("Воркер не запущен")
                val r = worker.reader ?: throw TtsInferenceException("Воркер не запущен")

                val outputFile = File(ServerConfig.tempDir, "piper_tts_${UUID.randomUUID()}.wav")
                // speed 1.0 = обычный темп, >1 быстрее. length_scale у Piper — обратная величина.
                val request = Json.encodeToString(
                    WorkerRequest.serializer(),
                    WorkerRequest(text, outputFile.absolutePath, length_scale = 1.0 / speed)
                )

                withTimeout(ServerConfig.inferenceTimeoutSeconds * 1000) {
                    w.write(request)
                    w.newLine()
                    w.flush()

                    val responseLine = r.readLine()
                        ?: throw TtsInferenceException("Воркер не ответил (проверь логи [piper_worker:$language])")

                    val response = Json.decodeFromString(WorkerResponse.serializer(), responseLine)

                    if (!response.success) {
                        throw TtsInferenceException("Ошибка в воркере: ${response.error}")
                    }
                    if (!outputFile.exists() || outputFile.length() == 0L) {
                        throw TtsInferenceException("Воркер отчитался успехом, но файл не создан/пуст")
                    }
                }
                outputFile
            }
        }
    }
}

class TtsInferenceException(message: String) : Exception(message)
