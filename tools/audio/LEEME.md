# Herramientas de audio (opcional)

No hacen falta para compilar la app. Sirven para regenerar o verificar los audios de `app/src/main/assets/audio`.

- `make_audio.py`: genera letras, "double" y las 30 palabras con Kokoro v1.0 (voz am_adam). Necesita `kokoro-onnx`, los archivos
  `kokoro-v1.0.onnx` y `voices-v1.0.bin` (GitHub: thewh1teagle/kokoro-onnx, release model-files-v1.0).
- `seq_validate.py` y `asr.py`: comprueban con Whisper (sherpa-onnx) que cada letra se entiende dentro de una secuencia.
- Después de generar, cada clip se convierte a OGG en tres velocidades con ffmpeg (`atempo`), y se escribe `manifest.txt`
  con la duración en milisegundos de cada archivo.
