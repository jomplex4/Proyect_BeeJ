# BEE J

App Android (Kotlin + Jetpack Compose) para preparar el **concurso de deletreo en inglés** del colegio
José Gabriel Condorcanqui (3.º, 4.º y 5.º de secundaria): **30 palabras, plan de 14 días**.
Marca: DIGITALMINDS. Creador: Joel Mamani Pauccara.

Todo el aprendizaje funciona **sin internet**: audios, deletreo, reconocimiento de voz, progreso.

## Cómo publicarla (VS Code + GitHub)

Primera vez (cambia `USUARIO` y `REPOSITORIO`):

```
git init
git branch -M main
git remote add origin https://github.com/USUARIO/REPOSITORIO.git
git add -A
git commit -m "BEE J 1.0: primera version"
git push -u origin main
git tag v1.0
git push origin v1.0
```

Versiones siguientes (antes sube `versionCode` y `versionName` en `app/build.gradle.kts`):

```
git add -A
git commit -m "BEE J x.y: descripcion"
git push
git tag vx.y
git push origin vx.y
```

GitHub Actions compila solo. En la pestaña **Actions** de cada ejecución están los APK (debug y release) y un resumen
con el tamaño real, las carpetas `lib/` (32 y 64 bits) y la huella de la firma. Con un tag `vX.Y` se crea una **Release**
con `BEE-J-vX.Y.apk`. Si el paso rojo falla, envíame el log de ese paso.

## Qué hay dentro

| Módulo | Qué hace |
|---|---|
| Aprender | Las 30 palabras. Escuchar palabra y deletreo (letra por letra). Velocidad de palabra (Lento, Normal, Rápido) y de deletreo (Normal, Rápido, Muy rápido, Extra rápido, hasta unas 3 letras por segundo como en campeonatos). Formato "F, F" o "DOUBLE F". |
| Entrenar | Oral (hablas y la app compara letra por letra) y escrito (deletreo con teclado propio, sin pistas de longitud). |
| Concurso | Simula el real: ves la palabra ~2,3 s, suena, desaparece y dices **PALABRA, deletreo, PALABRA**. Un error termina la ronda. |
| Repasar errores | Una palabra sale de la lista tras **3 aciertos seguidos desde el último error, en al menos 2 días distintos**. |
| Alfabeto | 26 letras con sonido e IPA, y pares que las reglas piden enfatizar: B/V, M/N, C/Z, T/D (más A/I y G/J). |
| Método | Pasos enumerados, ejemplo y plan de 14 días. |

## Decisiones técnicas

- **Voz (offline):** Vosk con el modelo `vosk-model-small-en-us-0.15` (Apache 2.0). Con letras sueltas acertaba muy poco
  (4 de 16 palabras en mis pruebas), así que ahora cada intento se lee DOS veces: (1) con una **gramática hecha a la medida de
  la palabra** (deletreo correcto, "double", y los errores probables: letra parecida, omitida, repetida o cambiada de lugar; en el
  Concurso también palabra inicial y final) y (2) con una gramática libre que hace de **veto**. Si la lectura libre oye otra letra,
  la app pide repetir en vez de dar por bueno el deletreo. Un error solo se marca si ambas lecturas coinciden en la posición.
  Resultados con audio de prueba SINTÉTICO (no voces reales): deletreos correctos 44 aceptados, 13 pidieron repetir, 7 marcados
  como error por equivocación; deletreos con error a propósito 25 marcados, 5 pidieron repetir, 2 aceptados por equivocación.
  Letras que suenan casi igual (B/V, C/Z, A/I) son el punto débil. El modelo (~40 MB) lo descarga GitHub Actions al compilar.
- **Normalización:** "bee/be" = B, "see/sea" = C, "are" = R, "you" = U, "why" = Y, "double u" = W, "double f" = FF, etc.
  Si el reconocedor oye una letra con poca seguridad, la app pide repetir en vez de contarla como error.
- **Audio (offline):** 26 letras, "double" y 30 palabras como archivos `.ogg` en `app/src/main/assets/audio/`
  (nombre `l_a_n.ogg` = letra A, normal; sufijos `s` lento, `n` normal, `f` rápido). Cada letra es un archivo propio.
  Para reemplazar cualquier audio por una grabación humana basta **guardar un archivo con el mismo nombre** y
  actualizar su duración en `manifest.txt` (milisegundos).
- **Velocidades:** palabra y deletreo son independientes. En deletreo cambian el ritmo y la pausa entre letras
  (Normal 650 ms, Rápido 330 ms, Muy rápido 160 ms, Extra rápido 60 ms con la letra acelerada 1,7 veces).
- **Arquitectura:** una sola Activity, ViewModel + StateFlow, sin librerías de íconos. La lógica crítica está en
  `app/src/main/java/com/digitalminds/beej/core/` (Kotlin puro, con pruebas en `app/src/test`).
- **Compatibilidad:** `minSdk 31` (Android 12), `compileSdk/targetSdk 35`, AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, JDK 17.
  ABIs: `armeabi-v7a` (Galaxy A03 Core, 32 bits) y `arm64-v8a`. Orientación vertical fija.

## Fuentes y licencias

- Letras A a Z: **grabaciones humanas** en inglés de EE. UU. de Joshua Shreve (2019), proyecto GCompris, licencia **GPL v3 o posterior**
  (https://github.com/KDE/gcompris-data, voices/en_US/alphabet). Por esa licencia, si publicas la app, publica también su código bajo GPL.
  Ver `app/src/main/assets/audio/LICENSES.txt`.
- Palabras y "double": **Kokoro v1.0** (pesos Apache 2.0), voz estadounidense `am_adam`. Confirma la licencia vigente en la página oficial del modelo.
- Voz: Vosk (Apache 2.0) y modelo pequeño en inglés (Apache 2.0). JNA (Apache 2.0 / LGPL 2.1).
- Fuentes tipográficas: Space Grotesk, Michroma, Atkinson Hyperlegible (SIL Open Font License; textos en `assets/fonts-licenses`).
- Logos: material propio de BEE J y DIGITALMINDS.

## Base pedagógica (de mi conocimiento, no de lectura nueva)

- **Práctica de recuperación** (deletrear sin mirar, corrección inmediata): Roediger y Karpicke, *Psychological Science*, 2006.
- **Práctica espaciada** (repasar falladas hoy y mañana, no el mismo minuto): Cepeda y otros, *Psychological Bulletin*, 2006.
- **Mezcla de palabras e intensidad creciente** en los días 11 a 14, y simulación del formato real.
- **Reglas del concurso del colegio** (documento adjunto): decir la palabra, deletrear y repetirla; "Could you repeat the word, please?";
  una letra de más, de menos o cambiada anula la palabra; énfasis en m/n, b/v, c/z, t/d.

## Firma y keystore

`app/beej-release.jks` (alias `beej`, contraseña `beej2026ks`, escrita en `app/build.gradle.kts`) es una firma
**personal para uso del colegio**, no apta para publicar en una tienda. **Respalda ese archivo**: si lo pierdes, no podrás
actualizar la app encima de la anterior (habría que desinstalarla). Debug y release usan la misma firma.
Para una firma de producción, crea otra keystore, no la subas a un repositorio público y mueve las contraseñas a secretos de GitHub.

## Importante: CAREFUL

La hoja impresa del colegio dice `CARIFUL`; la app usa **CAREFUL**. Confirma con el profesor qué grafía aceptará el jurado.
Para cambiarla en toda la app: `Words.kt` (texto) y los audios `w_careful_*` no necesitan cambio (la voz dice "careful").
