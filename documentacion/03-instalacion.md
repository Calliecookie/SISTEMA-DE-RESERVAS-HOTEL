# Entregable 6: Instalación y ejecución

## Requisitos

- JDK 21
- Maven 3.9 o Apache NetBeans 25
- MySQL 8
- Tomcat 10.1, solo si despliegas el archivo WAR

## Ejecutar desde NetBeans (sin configuración de servidor)

1. Instala JDK 21, MySQL Server 8.0 y MySQL Workbench.
2. En NetBeans: *File → Open Project* y elige la carpeta del proyecto. El archivo `nbactions.xml` ya indica que **Run** ejecuta `spring-boot:run`, así que no hace falta configurar Tomcat.
3. En MySQL Workbench abre `base-datos/esquema.sql`, ejecútalo con el rayo y repite con `base-datos/datos-iniciales.sql`.
4. En la carpeta del proyecto copia `.env.example` como `.env` y completa:
   - `BD_CONTRASENA`: la clave de tu MySQL.
   - `ADMIN_INICIAL_CONTRASENA`: mínimo 10 caracteres, con letras y números.
   - `ADMIN_CLAVE_RECUPERACION`: mínimo 12 caracteres.
   - `DEMO_CONTRASENA`: mínimo 8 caracteres.
5. Haz clic en **Run** y abre `http://localhost:8080`. La primera vez la aplicación crea el administrador y los datos de demostración.

Si el puerto 8080 está ocupado, agrega `PUERTO=8081` en `.env`.

## 1. Crear la base de datos

En MySQL Workbench, ejecuta en este orden:

1. `base-datos/esquema.sql`
2. `base-datos/datos-iniciales.sql`

Por consola:

```bash
mysql -u root -p < base-datos/esquema.sql
mysql -u root -p < base-datos/datos-iniciales.sql
```

**Atención:** `esquema.sql` borra las tablas si ya existen.

## 2. Configurar el entorno

Copia `.env.example` como `.env` y completa al menos:

```properties
BD_USUARIO=root
BD_CONTRASENA=tu_contrasena
ADMIN_INICIAL_CORREO=admin@hotelintiwasi.pe
ADMIN_INICIAL_CONTRASENA=elige-una-clave-2026
ADMIN_CLAVE_RECUPERACION=otra-clave-larga-para-recuperar-2026
DEMO_HABILITADA=true
DEMO_CONTRASENA=elige-otra-clave-2026
```

En Windows, si escribes rutas en `.env`, usa `/` en lugar de `\`.

## 3. Ejecutar

**Con Maven:**

```bash
mvn spring-boot:run
```

**En NetBeans:** abre la carpeta del proyecto y ejecuta la meta `spring-boot:run` desde *Run Maven > Goals*. El archivo `.env` debe estar en la raíz del proyecto.

Abre `http://localhost:8080`.

## 4. Desplegar en Tomcat 10.1

```bash
mvn -DskipTests package
```

1. Copia `target/sistema-reservas-hotel.war` a la carpeta `webapps` de Tomcat.
2. Crea la carpeta `.hotel-reservas` en tu carpeta de usuario.
3. Copia allí el archivo `.env`.
4. Define `URL_PUBLICA=http://localhost:8080/sistema-reservas-hotel`.
5. Inicia Tomcat y abre esa dirección.

Tomcat no lee el `.env` de la raíz del proyecto, porque trabaja desde otra carpeta. Por eso se usa `~/.hotel-reservas/.env`.

## Datos de acceso de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | Valor de `ADMIN_INICIAL_CORREO` | Valor de `ADMIN_INICIAL_CONTRASENA` |
| Trabajador | `recepcion@hotelintiwasi.pe` | Valor de `DEMO_CONTRASENA` |
| Cliente | `cliente@hotelintiwasi.pe` | Valor de `DEMO_CONTRASENA` |

Las contraseñas no están escritas en el código. Se definen en `.env` y se guardan con Argon2id. Desactiva `DEMO_HABILITADA` fuera de la demostración.

## Configurar SMTP con Gmail

1. Activa la verificación en dos pasos en tu cuenta de Google.
2. Crea una contraseña de aplicación.
3. Completa `.env`:

```properties
CORREO_HABILITADO=true
CORREO_REMITENTE=tu_correo@gmail.com
SMTP_SERVIDOR=smtp.gmail.com
SMTP_PUERTO=587
SMTP_USUARIO=tu_correo@gmail.com
SMTP_CONTRASENA=contrasena_de_aplicacion
```

Sin SMTP, las reservas funcionan y el comprobante queda disponible en el sistema, pero **no se envían códigos de recuperación de contraseña**.

## Configurar Factiliza

1. Crea tu cuenta en [Factiliza](https://app.factiliza.com/api-consulta/) y obtén un token.
2. Define `FACTILIZA_TOKEN` en `.env`.

Factiliza es un servicio externo, y sus planes y límites dependen de tu cuenta. El sistema llama a `GET https://api.factiliza.com/v1/dni/info/{dni}` desde el backend, con el token en la cabecera `Authorization: Bearer`. Sin token, el registro pide los nombres manualmente mientras `FACTILIZA_PERMITIR_MANUAL=true`.

## Configurar PayPal

1. Entra a [PayPal Developer](https://developer.paypal.com/) y crea una aplicación.
2. Copia el Client ID y el Secret del entorno de pruebas.
3. Completa `.env`:

```properties
PAYPAL_ENTORNO=sandbox
PAYPAL_CLIENTE_ID=tu_client_id
PAYPAL_SECRETO=tu_secret
URL_PUBLICA=http://localhost:8080
```

4. Reinicia la aplicación.
5. En *Administración > Configuración*, define el tipo de cambio en soles por dólar.

PayPal no admite soles, por eso el cobro se hace en dólares. Revisa el tipo de cambio con frecuencia: lo fijas tú y el sistema no lo actualiza solo. Para producción, usa credenciales `live` y `PAYPAL_ENTORNO=live`.

**Reembolsos:** al cancelar una reserva pagada, el sistema registra el reembolso, pero la devolución del dinero se hace manualmente desde PayPal, o por el mismo medio en efectivo o Yape.

## Configurar Yape

En *Administración > Configuración*:

1. Escribe el número de celular y el titular.
2. Sube la imagen del código QR.

El cliente paga desde su app y registra el número de operación. Un trabajador revisa la operación en la app de Yape y confirma el pago. No encontramos una API pública oficial de Yape para comercios. Automatizarlo requiere contratar un procesador autorizado.

## Tarea programada

La expiración de reservas corre dentro de la aplicación cada 60 segundos. No necesita cron. Se ajusta con `TAREA_EXPIRACION_MS` y se desactiva con `TAREAS_HABILITADAS=false`.

El cierre del día corre a las 23:59 (`TAREA_CIERRE_DIA_CRON`): da de baja y elimina los pedidos que no se pagaron, finaliza las estancias cuya salida ya pasó y recalcula el estado de las habitaciones. Para conservar los pedidos expirados en lugar de borrarlos usa `TAREA_ELIMINAR_NO_PAGADAS=false`.

Si el administrador olvida su contraseña, entra a `/recuperar-contrasena/administrador` (enlace en la pantalla de ingreso). Puede pedir un código por correo o, si el correo no está configurado, usar la clave `ADMIN_CLAVE_RECUPERACION`.

## Ejecutar las pruebas

Las pruebas de integración necesitan MySQL, porque verifican bloqueos reales. Usan la base `hotel_reservas_prueba` y la crean si no existe.

```bash
PRUEBA_BD_USUARIO=root PRUEBA_BD_CONTRASENA=tu_contrasena mvn test
```

En Windows PowerShell:

```powershell
$env:PRUEBA_BD_USUARIO="root"; $env:PRUEBA_BD_CONTRASENA="tu_contrasena"; mvn test
```

Necesitan libres los puertos 3025, 39091 y 39092.

## Problemas comunes

| Problema | Solución |
|---|---|
| `Access denied for user` | Revisa `BD_USUARIO` y `BD_CONTRASENA` |
| `Unknown database` | Ejecuta `base-datos/esquema.sql` |
| No puedo iniciar sesión como administrador | Revisa el registro de arranque: la contraseña debe tener 10 caracteres, letras y números |
| PayPal o Yape no aparecen | Revisa *Administración > Configuración > Estado de las integraciones* |
| El navegador pierde la sesión con HTTP | Usa `SESION_COOKIE_SEGURA=false` en local |
| Las fotos no aparecen | Las del catálogo vienen incluidas en el proyecto. Si cambiaste una por una URL de Wikimedia Commons, esa sí requiere internet |
| Las tildes se ven como `Ã¡` | La base se cargó con un cliente en `latin1`. Ejecuta de nuevo `esquema.sql` y `datos-iniciales.sql`: ambos empiezan con `SET NAMES utf8mb4` |
