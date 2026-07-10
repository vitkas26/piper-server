#!/usr/bin/env python3
"""
Персистентный воркер Piper — та же схема, что whisper_worker.py в whisper-server:
голос грузится один раз, дальше построчный JSON-протокол через stdin/stdout.

Piper значительно легче Whisper (это TTS с относительно небольшой ONNX-моделью,
не тяжёлый transformer), так что персистентность тут даёт меньше выигрыша, чем для
Whisper — но архитектурно проще держать все три сервера (akylai/whisper/piper)
на одном паттерне, чем каждый раз изобретать заново.

Протокол:
  stdin:  {"text": "текст для озвучки", "output_path": "/tmp/out.wav"}
  stdout: {"success": true} или {"success": false, "error": "..."}

Запуск (Kotlin делает это сам):
    python piper_worker.py /path/to/voice-model.onnx
"""
import sys
import json
import wave
import warnings

warnings.filterwarnings("ignore")


def main():
    if len(sys.argv) < 2:
        print("Usage: piper_worker.py <voice_model.onnx>", file=sys.stderr)
        sys.exit(1)

    voice_model_path = sys.argv[1]

    print(f"Загружаю голос {voice_model_path}...", file=sys.stderr)

    try:
        from piper import PiperVoice
        from piper.config import SynthesisConfig
    except ImportError:
        print("Не установлен piper-tts, см. python/requirements.txt", file=sys.stderr)
        sys.exit(1)

    try:
        voice = PiperVoice.load(voice_model_path)
    except Exception as e:
        print(
            f"Не удалось загрузить голос: {e}. Проверь, что рядом с .onnx лежит "
            f"файл .onnx.json (конфиг голоса) — Piper требует оба файла вместе. "
            f"См. README про скачивание голосов.",
            file=sys.stderr,
        )
        sys.exit(1)

    print("Голос загружен, жду запросы на stdin...", file=sys.stderr)

    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue

        try:
            request = json.loads(line)
            text = request["text"]
            output_path = request["output_path"]
            length_scale = request.get("length_scale")

            syn_config = SynthesisConfig(length_scale=length_scale) if length_scale else None

            with wave.open(output_path, "wb") as wav_file:
                voice.synthesize_wav(text, wav_file, syn_config=syn_config)

            response = {"success": True}
        except Exception as e:
            response = {"success": False, "error": str(e)}

        print(json.dumps(response, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    main()
