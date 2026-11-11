# Radio La Grande de Huétamo — Android Studio

Proyecto nativo Kotlin + Jetpack Compose para Android 17 (API 37).

## Incluye
- Pantalla principal inspirada en la referencia.
- Reproductor de audio con Media3/ExoPlayer.
- Panel administrativo con PIN inicial 1069.
- Edición de nombre, frecuencia, eslogan y URL de streaming.
- Edición de enlaces Facebook, WhatsApp y sitios web.
- Alta, edición y eliminación de enlaces adicionales.
- Selección de fondo y logotipo desde el teléfono.
- Persistencia local de ajustes.

## Abrir en Android Studio
1. Descarga el repositorio como ZIP o clona el repositorio.
2. Abre la carpeta `RadioLaGrande` en Android Studio.
3. Instala Android SDK Platform 37.
4. Usa JDK 17 y sincroniza Gradle.
5. Ejecuta la app en un dispositivo o emulador.

## Configuración
1. Abre la app y pulsa “Abrir administración”.
2. PIN inicial: `1069`.
3. Selecciona el logotipo y el fondo desde el teléfono.
4. Introduce la URL real del streaming.
5. Completa los enlaces de redes y sitios oficiales.
6. Guarda los cambios.

## Notas
- El streaming no funcionará hasta introducir una URL de audio válida (Icecast/Shoutcast MP3/AAC).
- Los archivos gráficos originales no se incluyen en esta primera subida; se pueden seleccionar desde el panel.
- Cambia el PIN inicial antes de distribuir la aplicación.
- Esta es la fuente inicial editable, no un APK compilado ni una versión probada en dispositivo.


## Sincronización remota con el panel web

La rama `radio-la-grande-sync` incorpora la consulta de configuración remota mediante `RadioApi.kt`. La app descarga estación, frecuencia, streaming, redes, WhatsApp, web, logotipo, fondo, colores y enlaces al iniciar, y permite solicitar una actualización desde Administración.

Antes de compilar la versión conectada:
1. Sube la carpeta `cpanel/` al hosting con HTTPS.
2. Crea la base de datos MySQL e importa `cpanel/database.sql`.
3. Configura `cpanel/config.php` con credenciales y hash de administrador.
4. Edita `AppConfig.kt` y reemplaza `TU-DOMINIO.com` por el dominio real.
5. Entra al panel web, guarda la configuración y luego pulsa **Sincronizar con panel web** en la app.

La sincronización es de lectura desde el panel hacia Android. Los cambios hechos en el panel local de la app no se suben a MySQL. El chat requiere una integración aparte.
