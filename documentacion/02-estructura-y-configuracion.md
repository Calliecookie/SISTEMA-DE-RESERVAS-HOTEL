# Entregable 3: Estructura y configuración

## Árbol del proyecto

```text
sistema-reservas-hotel/
├── base-datos/
│   ├── esquema.sql
│   └── datos-iniciales.sql
├── documentacion/
├── almacenamiento/
├── src/main/java/pe/edu/josepardo/hotel/
│   ├── AplicacionHotel.java
│   ├── configuracion/
│   ├── controlador/
│   │   ├── api/
│   │   └── vista/
│   ├── dto/
│   ├── excepcion/
│   ├── modelo/
│   ├── repositorio/
│   ├── seguridad/
│   ├── servicio/
│   └── tarea/
├── src/main/resources/
│   ├── application.yml
│   ├── recursos/
│   │   ├── css/
│   │   ├── js/
│   │   ├── fuentes/
│   │   ├── iconos/
│   │   └── imagenes/
│   │       └── habitaciones/
│   └── vistas/
│       ├── administrador/
│       ├── cliente/
│       ├── componentes/
│       ├── correo/
│       ├── documentos/
│       └── trabajador/
├── src/main/webapp/
│   ├── META-INF/context.xml
│   └── WEB-INF/web.xml
├── src/test/
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

La carpeta `src/main/java` sigue la convención de Maven. Los paquetes propios, las tablas, las rutas, los métodos y las variables están en español.

## Módulos

| Paquete | Responsabilidad |
|---|---|
| `controlador.vista` | Devuelve las páginas Thymeleaf y entrega archivos |
| `controlador.api` | API JSON consumida con Fetch |
| `servicio` | Reglas de negocio, transacciones e integraciones |
| `repositorio` | Acceso a datos: JPA para escrituras y SQL parametrizado para consultas |
| `modelo` | Entidades y enumeraciones |
| `dto` | Objetos de entrada y salida, con validaciones |
| `seguridad` | Autenticación, límites de solicitudes y control de tráfico |
| `configuracion` | Seguridad, propiedades y filtros |
| `excepcion` | Errores de negocio y manejo centralizado |
| `tarea` | Expiración automática de reservas |

## Dependencias principales

| Dependencia | Uso |
|---|---|
| Spring Boot 3.5.16 | Web, seguridad, datos, validación y correo |
| Thymeleaf | Vistas y plantillas de PDF y correo |
| MySQL Connector/J | Conexión a MySQL |
| Bouncy Castle 1.80 | Requerido por Argon2id |
| OpenHTMLtoPDF 1.1.37 | Generación de PDF |
| Apache POI 5.4.1 | Exportación a Excel |
| GreenMail 2.1.3 | Servidor SMTP real para pruebas |

Las fuentes Inter y Lora, y los iconos Lucide, están incluidos en el proyecto. La interfaz no necesita un CDN.

## Variables de entorno

Copia `.env.example` como `.env`. El archivo `.env` nunca se sube a Git.

| Variable | Obligatoria | Descripción |
|---|---|---|
| `BD_URL`, `BD_USUARIO`, `BD_CONTRASENA` | Sí | Conexión a MySQL |
| `ADMIN_INICIAL_CORREO`, `ADMIN_INICIAL_CONTRASENA` | Primer arranque | Crea el administrador. La contraseña necesita 10 caracteres, letras y números |
| `ADMIN_CLAVE_RECUPERACION` | Recuperación del administrador | Clave de al menos 12 caracteres para restablecer la contraseña del administrador sin correo. Vacía, desactiva ese método |
| `TAREA_CIERRE_DIA_CRON`, `TAREA_ELIMINAR_NO_PAGADAS` | Cierre del día | Hora del cierre (por defecto `0 59 23 * * *`) y si se eliminan los pedidos no pagados |
| `DEMO_HABILITADA`, `DEMO_CONTRASENA` | No | Crea datos y cuentas de demostración |
| `URL_PUBLICA` | Para PayPal | Dirección pública, incluido el contexto si lo hay |
| `SESION_COOKIE_SEGURA` | Producción | `true` cuando el sitio usa HTTPS |
| `FACTILIZA_TOKEN` | No | Token de la API de DNI |
| `FACTILIZA_PERMITIR_MANUAL` | No | Permite el registro manual si Factiliza no responde |
| `CORREO_HABILITADO`, `CORREO_REMITENTE` | No | Activa el envío de correos |
| `SMTP_SERVIDOR`, `SMTP_PUERTO`, `SMTP_USUARIO`, `SMTP_CONTRASENA` | Con correo | Servidor SMTP |
| `PAYPAL_ENTORNO` | Con PayPal | `sandbox` o `live` |
| `PAYPAL_CLIENTE_ID`, `PAYPAL_SECRETO` | Con PayPal | Credenciales de la aplicación de PayPal |
| `PAYPAL_MONEDA` | No | Moneda de cobro, `USD` por defecto |
| `TRAFICO_*` | No | Límites de la protección de tráfico |
| `TOMCAT_HILOS_MAXIMOS`, `TOMCAT_CONEXIONES_MAXIMAS` | No | Límites del Tomcat embebido |
| `ALMACENAMIENTO_RUTA` | No | Carpeta de comprobantes e imágenes |
