# Sistema de Reservas Hotel Inti Wasi

Plataforma web para reservar habitaciones en línea y administrar la operación de un hotel. Proyecto de la unidad didáctica Taller de Programación Web, IESTP "José Pardo", 2026-II.

**Versión 3.0.0** (10 de noviembre de 2026). Las novedades y correcciones están en el [registro de cambios](CHANGELOG.md).

## Estado de verificación

Verificado el 2 de noviembre de 2026 en Linux con JDK 21, Maven 3.9, MySQL 8.0.46, Chromium y Tomcat 10.1.61.

| Verificación | Resultado |
|---|---|
| Compilación con Maven | Aprobada |
| 125 pruebas automatizadas: 32 unitarias y 93 de integración contra MySQL, incluida la concurrencia de reservas | Aprobadas (10 de noviembre de 2026) |
| Comprobantes PDF y reportes en PDF y Excel | Generados y revisados |
| Envío de correo con el comprobante adjunto | Verificado con un servidor SMTP de pruebas (GreenMail) |
| Recorrido de las 21 pantallas de los tres roles en Chromium, escritorio y móvil | Sin errores de JavaScript, respuestas fallidas, imágenes rotas ni desborde horizontal |
| Flujo completo en el navegador: reserva, pago en efectivo, confirmación del trabajador, comprobante y exportación de reportes | Aprobado |
| Despliegue del WAR en Tomcat 10.1 con la ruta `/sistema-reservas-hotel` | Aprobado |
| Fotografías: carga y licencia de cada archivo | 12 fotos con licencia CC BY o CC BY-SA, incluidas en el proyecto y atribuidas en `/creditos` |
| Envío por un servidor SMTP real | **Pendiente**: requiere tu cuenta de correo |
| Factiliza y PayPal con credenciales reales | **Pendiente**: requieren tus credenciales. Las pruebas usan servidores simulados con las respuestas documentadas |

## Tecnologías

Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Thymeleaf, MySQL 8, OpenHTMLtoPDF, Apache POI, Jakarta Mail, Maven y Tomcat 10.1.

Spring Boot 3.5 es la última línea compatible con Tomcat 10.1. Su soporte gratuito terminó el 30 de junio de 2026, así que un despliegue comercial debería migrar a Spring Boot 4 con Tomcat 11.

## Inicio rápido

Con JDK 21 y MySQL 8 encendido (usuario `root` sin contraseña), abre el proyecto en NetBeans y pulsa **Run**, o ejecuta:

```bash
mvn spring-boot:run
```

El primer arranque crea la base de datos, las tablas y los datos de demostración. Abre `http://localhost:8080` y entra con `admin@hotelintiwasi.pe` / `AdminHotel2026`.

Si tu MySQL tiene contraseña, o quieres activar el correo y la consulta de DNI, crea un `.env` a partir de `.env.example`.

Si ya tenías la base creada con la versión anterior, ejecuta una vez `mysql -u root -p < base-datos/migracion-estado-habitaciones.sql`.

## Funciones

**Cliente:** catálogo con filtros, disponibilidad por fechas, registro con DNI, carrito, reserva en cinco etapas, pago, comprobantes y recuperación de contraseña.

**Trabajador:** pagos por verificar, llegadas, salidas e ingreso y salida de huéspedes.

**Administrador:** dashboard, habitaciones con su estado (Disponible, Reservada, Ocupada, En mantenimiento, Fuera de servicio), categorías, clientes, usuarios, ventas, actualización del estado de las reservas desde el listado, seis reportes con PDF y Excel, configuración, auditoría y recuperación de su contraseña por correo o con la clave de recuperación.

**Comprobante:** PDF con el total en letras, enviado al correo del cliente.

**Cierre del día:** a las 23:59 los pedidos no pagados se eliminan y las estancias vencidas se finalizan.

**Pagos:** efectivo, PayPal y Yape. PayPal y Yape aparecen cuando están configurados.

## Documentación

| Documento | Contenido |
|---|---|
| [Registro de cambios](CHANGELOG.md) | Novedades y correcciones de cada versión |
| [Análisis y diseño](documentacion/01-analisis-y-diseno.md) | Requerimientos, reglas de negocio y diagramas |
| [Estructura y configuración](documentacion/02-estructura-y-configuracion.md) | Módulos, dependencias y variables |
| [Instalación](documentacion/03-instalacion.md) | MySQL, Tomcat, SMTP, Factiliza, PayPal y Yape |
| [Contratos de API](documentacion/04-contratos-api.md) | Rutas y formato de respuestas |
| [Protección contra DDoS](documentacion/05-proteccion-ddos.md) | Capas, límites y alcances |
| [Pruebas](documentacion/06-pruebas.md) | Matriz de cobertura |
| [Guion de sustentación](documentacion/07-guion-sustentacion.md) | Demostración de los seis hitos |
| [Informe de titulación](documentacion/informe/Informe-Proyecto-de-Titulacion-Hotel-Inti-Wasi.docx) | Informe completo en Word, con versión en [PDF](documentacion/informe/Informe-Proyecto-de-Titulacion-Hotel-Inti-Wasi.pdf) |
