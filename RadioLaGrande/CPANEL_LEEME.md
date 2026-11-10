# Panel web cPanel — La Grande de Huétamo

Este directorio contiene el backend PHP y la base MySQL para administrar la configuración de la emisora desde un navegador.

## Archivos
- admin.php: inicio de sesión y panel para editar identidad, streaming, redes, botones, logo y fondo.
- api.php: endpoint JSON de configuración para la app.
- config.php: credenciales de base de datos y hash de acceso.
- database.sql: tablas y configuración inicial.
- .htaccess: evita listar directorios y bloquea el acceso directo a archivos sensibles.

## Instalación
1. Crea una base de datos MySQL y usuario en cPanel.
2. Importa database.sql con phpMyAdmin.
3. Edita config.php con los datos de la base.
4. Genera un hash seguro en PHP: password_hash('TU_CLAVE', PASSWORD_DEFAULT). Colócalo en ADMIN_PASSWORD_HASH y elimina el script temporal.
5. Sube el contenido de cpanel/ a public_html/radio/ (o una carpeta equivalente).
6. Abre https://TU-DOMINIO/radio/admin.php.
7. Configura una URL real de streaming Icecast/Shoutcast, redes, logo y fondo.

## Conexión con Android
El endpoint de configuración queda en https://TU-DOMINIO/radio/api.php.
La app Android de este repositorio incluye su propio panel local; para que sus controles se sincronicen con este panel web, hay que integrar el consumo de api.php en la app y probarlo con el dominio y stream reales. No se debe asumir que los cambios de cPanel ya se reflejan automáticamente en la app sin esa integración.

## Seguridad
- Usa HTTPS.
- Cambia las credenciales de ejemplo.
- No publiques datos reales de acceso.
- El reproductor requiere la URL real del streaming.
