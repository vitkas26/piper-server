# Piper Server (self-hosted TTS, локальный тест на Mac)

Отдельный Ktor-сервис — self-hosted TTS через Piper. Порт **8002** (akylai-server —
8000, whisper-server — 8001, все три могут работать одновременно на одном Mac).

## Зачем это рядом с облачным/системным TTS

Piper — открытый, бесплатный, и, в отличие от Whisper-моделей, специально спроектирован
для слабого железа (используется на Raspberry Pi-класса устройствах). Практический
смысл: если на финальной LG-панели (без GMS, слабый Cortex-A55) вообще не окажется
работающего TTS-движка — Piper, возможно, единственный вариант, который реально
потянет **прямо на панели**, а не только на сервере. Стоит послушать качество уже
сейчас, прежде чем на это рассчитывать.

Честно: звучит заметно более "механически", чем облачные премиальные голоса
(Azure Neural, ElevenLabs) — для имиджевого продукта это компромисс, который стоит
оценить на слух, а не считать заранее решённым.

## Быстрый старт

```bash
bash python/setup.sh   # venv + зависимости + скачивание тестового русского голоса
./gradlew run
```

```bash
curl http://localhost:8002/health
# -> ok

curl -X POST -H "Content-Type: application/json" \
  -d '{"text":"Здравствуйте, это тестовое сообщение","language":"ru","speed":1.2}' \
  http://localhost:8002/tts/synthesize --output test.wav
```

`language` и `speed` — необязательные поля. `language` по умолчанию `ru` (см.
`PIPER_DEFAULT_LANG`). `speed` по умолчанию `1.0`, `>1` — быстрее, `<1` — медленнее
(внутри маппится в piper'овский `length_scale = 1/speed`).

## Языки

Один персистентный python-воркер на язык, поднимается лениво при первом запросе на
этот язык. Все 5 голосов скачаны и проверены вживую (`python -m piper.download_voices
--download-dir models <voice-name>`):

| язык | env override    | путь по умолчанию                  |
|------|-----------------|-------------------------------------|
| ru   | PIPER_VOICE_RU  | models/ru_RU-irina-medium.onnx      |
| en   | PIPER_VOICE_EN  | models/en_US-lessac-medium.onnx     |
| de   | PIPER_VOICE_DE  | models/de_DE-thorsten-medium.onnx   |
| zh   | PIPER_VOICE_ZH  | models/zh_CN-huayan-medium.onnx     |
| tr   | PIPER_VOICE_TR  | models/tr_TR-dfki-medium.onnx       |

Полный список голосов: `github.com/rhasspy/piper/blob/master/VOICES.md`. У каждого
голоса два файла — `.onnx` и `.onnx.json` — оба должны лежать рядом в `models/`.

**Кыргызского голоса у Piper нет** — для KY по-прежнему используем AkylAI-TTS-mini
(`akylai-server`, отдельный проект).
