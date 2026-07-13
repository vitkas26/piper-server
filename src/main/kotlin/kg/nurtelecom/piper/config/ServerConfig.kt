package kg.nurtelecom.piper.config

import java.io.File

object ServerConfig {

    val pythonExecutable: String =
        System.getenv("PIPER_PYTHON") ?: "${System.getProperty("user.dir")}/python/venv/bin/python"

    val workerScript: String =
        System.getenv("PIPER_WORKER_SCRIPT") ?: "${System.getProperty("user.dir")}/python/piper_worker.py"

    val tempDir: File =
        File(System.getenv("PIPER_TEMP_DIR") ?: System.getProperty("java.io.tmpdir")).apply { mkdirs() }

    val inferenceTimeoutSeconds: Long =
        System.getenv("PIPER_INFERENCE_TIMEOUT_SEC")?.toLongOrNull() ?: 30L

    val defaultLanguage: String = System.getenv("PIPER_DEFAULT_LANG") ?: "ru"

    // Каждый язык — отдельный .onnx голос, скачивается отдельно (python/setup.sh скачивает
    // только ru; Dockerfile качает все 5 при сборке образа). Полный список голосов:
    // https://github.com/rhasspy/piper/blob/master/VOICES.md
    // Переопределить путь: PIPER_VOICE_RU / PIPER_VOICE_EN / PIPER_VOICE_DE / PIPER_VOICE_ZH / PIPER_VOICE_TR.
    // Все 5 имён ниже скачаны и проверены вживую (см. README).
    private val modelsDir = "${System.getProperty("user.dir")}/models"

    val voiceModelPaths: Map<String, String> = mapOf(
        "ru" to (System.getenv("PIPER_VOICE_RU") ?: "$modelsDir/ru_RU-irina-medium.onnx"),
        "en" to (System.getenv("PIPER_VOICE_EN") ?: "$modelsDir/en_US-lessac-medium.onnx"),
        "de" to (System.getenv("PIPER_VOICE_DE") ?: "$modelsDir/de_DE-thorsten-medium.onnx"),
        "zh" to (System.getenv("PIPER_VOICE_ZH") ?: "$modelsDir/zh_CN-huayan-medium.onnx"),
        "tr" to (System.getenv("PIPER_VOICE_TR") ?: "$modelsDir/tr_TR-dfki-medium.onnx"),
    )
}
