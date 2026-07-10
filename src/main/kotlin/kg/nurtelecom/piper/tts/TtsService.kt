package kg.nurtelecom.piper.tts

import java.io.File

interface TtsService {
    suspend fun synthesize(text: String, language: String, speed: Double): File
}
