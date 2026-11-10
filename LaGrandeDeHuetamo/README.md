# La Grande de Huétamo — Android Studio

Proyecto base editable para la emisora La Grande de Huétamo.

## Estructura
- `android/`: aplicación nativa Kotlin + Jetpack Compose.
- `admin/index.html`: panel web administrativo inicial.
- `firebase/`: reglas de Firestore y Storage.
- `firebase.json`: configuración de Firebase Hosting.

## Requisitos
- Android Studio compatible con Android 17 / API 37.
- JDK 17.
- Proyecto Firebase propio.

## Configuración
1. Crea un proyecto en https://console.firebase.google.com/.
2. Registra una aplicación Android con package name `com.lagrande.huetamo`.
3. Descarga `google-services.json` y colócalo en `android/app/google-services.json`.
4. Activa Email/Password en Firebase Authentication.
5. Crea Firestore y Storage.
6. Reemplaza `ADMIN_EMAIL_AQUI` en las reglas y publícalas.
7. En `admin/index.html`, reemplaza el objeto `firebaseConfig` por la configuración de tu aplicación web Firebase.
8. Publica el directorio `admin` con Firebase Hosting.

## Abrir Android Studio
Abre la carpeta `LaGrandeDeHuetamo/android`. Instala SDK 37, sincroniza Gradle y ejecuta la aplicación.

## Enlace del panel
Cuando publiques el panel, copia su URL HTTPS y colócala en la constante `ADMIN_PANEL_URL` de `MainActivity.kt`.

## Nota
Este es un proyecto base que requiere configuración de Firebase y la URL real del streaming antes de estar operativo en producción.