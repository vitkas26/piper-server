# syntax=docker/dockerfile:1

# ---- Stage 1: сборка jar ----
FROM gradle:8.7-jdk17 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle buildFatJar --no-daemon

# ---- Stage 2: runtime ----
FROM eclipse-temurin:17-jre-jammy

# jammy (Ubuntu 22.04) в штатных репах даёт python3.10 — 3.11 берём из deadsnakes.
RUN apt-get update && apt-get install -y --no-install-recommends \
        software-properties-common gnupg curl \
    && add-apt-repository -y ppa:deadsnakes/ppa \
    && apt-get update && apt-get install -y --no-install-recommends \
        python3.11 python3.11-venv \
    && apt-get purge -y software-properties-common gnupg \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY python/requirements.txt python/requirements.txt
RUN python3.11 -m venv python/venv \
    && python/venv/bin/pip install --no-cache-dir --upgrade pip \
    && python/venv/bin/pip install --no-cache-dir -r python/requirements.txt

# Голоса качаем прямо в образ. ru_RU-irina-medium — единственное имя, реально
# проверенное вживую (см. python/setup.sh); en/de/zh/tr — предположение по
# конвенции именования Piper (VOICES.md), не проверенное. Каждый голос качаем
# отдельным RUN, чтобы отсутствие одного имени не валило всю сборку — вместо
# этого явный WARNING в лог сборки.
RUN mkdir -p /tmp/voices models

RUN python/venv/bin/python -m piper.download_voices --download-dir /tmp/voices ru_RU-irina-medium \
    && mv /tmp/voices/ru_RU-irina-medium.onnx /tmp/voices/ru_RU-irina-medium.onnx.json models/ \
    || echo "WARNING: голос ru_RU-irina-medium не скачался — проверь имя в VOICES.md"

RUN python/venv/bin/python -m piper.download_voices --download-dir /tmp/voices en_US-lessac-medium \
    && mv /tmp/voices/en_US-lessac-medium.onnx /tmp/voices/en_US-lessac-medium.onnx.json models/ \
    || echo "WARNING: голос en_US-lessac-medium не скачался — проверь имя в VOICES.md"

RUN python/venv/bin/python -m piper.download_voices --download-dir /tmp/voices de_DE-thorsten-medium \
    && mv /tmp/voices/de_DE-thorsten-medium.onnx /tmp/voices/de_DE-thorsten-medium.onnx.json models/ \
    || echo "WARNING: голос de_DE-thorsten-medium не скачался — проверь имя в VOICES.md"

RUN python/venv/bin/python -m piper.download_voices --download-dir /tmp/voices zh_CN-huayan-medium \
    && mv /tmp/voices/zh_CN-huayan-medium.onnx /tmp/voices/zh_CN-huayan-medium.onnx.json models/ \
    || echo "WARNING: голос zh_CN-huayan-medium не скачался — проверь имя в VOICES.md"

# tr_TR-dfki-medium — мужской голос (женского турецкого в Piper нет в принципе,
# известное открытое ограничение проекта). Имя самое неподтверждённое из пяти.
RUN python/venv/bin/python -m piper.download_voices --download-dir /tmp/voices tr_TR-dfki-medium \
    && mv /tmp/voices/tr_TR-dfki-medium.onnx /tmp/voices/tr_TR-dfki-medium.onnx.json models/ \
    || echo "WARNING: голос tr_TR-dfki-medium не скачался — проверь имя в VOICES.md"

RUN rm -rf /tmp/voices

COPY python/piper_worker.py python/piper_worker.py
COPY --from=build /home/gradle/project/build/libs/piper-server-all.jar app.jar

# ServerConfig.kt: пути по умолчанию и так укажут сюда (user.dir=/app), но
# фиксируем явно — не завязываемся на то, откуда сервис запущен.
ENV PIPER_PYTHON=/app/python/venv/bin/python
ENV PIPER_WORKER_SCRIPT=/app/python/piper_worker.py
ENV PIPER_VOICE_RU=/app/models/ru_RU-irina-medium.onnx
ENV PIPER_VOICE_EN=/app/models/en_US-lessac-medium.onnx
ENV PIPER_VOICE_DE=/app/models/de_DE-thorsten-medium.onnx
ENV PIPER_VOICE_ZH=/app/models/zh_CN-huayan-medium.onnx
ENV PIPER_VOICE_TR=/app/models/tr_TR-dfki-medium.onnx

EXPOSE 8002

CMD ["java", "-Xmx350m", "-jar", "app.jar"]
