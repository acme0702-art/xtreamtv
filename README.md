# Xtream TV (Android TV, 32 bits)

TV en directo, películas, series, EPG, favoritos y búsqueda.

## Generar el APK
**Opción A – Android Studio:** Abrir carpeta > esperar sincronización > Build > Build APK(s).
APK en `app/build/outputs/apk/debug/app-debug.apk`.

**Opción B – GitHub (sin instalar nada):** sube esta carpeta a un repositorio nuevo.
La pestaña *Actions* ejecuta "Build APK"; descarga el artefacto `XtreamTV-apk`.

## Instalar en la TV
`adb connect IP_TV:5555` y `adb install app-debug.apk`, o copia el APK a un USB.

## Mando
OK: reproducir · Mantener OK: favorito · ▲▼ en directo: cambiar canal.
