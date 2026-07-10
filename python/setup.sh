#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

echo "== Создаю venv =="
python3 -m venv venv
source venv/bin/activate

echo "== Ставлю зависимости =="
pip install --upgrade pip
pip install -r requirements.txt

echo "== Скачиваю русский голос (irina, medium quality) для теста =="
mkdir -p ../models
if [ ! -f "../models/ru_RU-irina-medium.onnx" ]; then
    python -m piper.download_voices ru_RU-irina-medium
    # download_voices кладёт файлы в текущую директорию по умолчанию — переносим в models/
    mv ru_RU-irina-medium.onnx ru_RU-irina-medium.onnx.json ../models/ 2>/dev/null || \
        echo "Не удалось переместить автоматически — проверь, куда скачался голос, и поправь PIPER_VOICE_MODEL в ServerConfig.kt"
fi

echo "=================================================================="
echo "Готово. Список остальных голосов (для др. языков) —"
echo "https://github.com/rhasspy/piper/blob/master/VOICES.md"
echo "Кыргызского голоса у Piper нет — для KY используем AkylAI-TTS-mini,"
echo "этот сервер только для остальных 5 языков."
echo "=================================================================="
