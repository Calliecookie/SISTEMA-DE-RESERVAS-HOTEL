# Sistema de Reservas Hotel Inti Wasi

Plataforma web para reservar habitaciones en línea y administrar la operación de un hotel. Proyecto de la unidad didáctica Taller de Programación Web, IESTP "José Pardo", 2026-II.

## Estado de verificación

**El proyecto aún no se ha compilado con Spring ni ejecutado contra MySQL.** El entorno donde se desarrolló no tenía acceso a Maven Central, MySQL, ni al computador del autor.

| Verificación | Resultado |
|---|---|
| 26 pruebas de lógica pura: importes, estados, fechas, tráfico | Ejecutadas y aprobadas, sobre sustitutos mínimos de Spring y JUnit |
| Sintaxis y símbolos propios de Java | Revisados con `javac`, sin las librerías de Spring |
| Sintaxis e importaciones de JavaScript | Revisadas |
| Interfaz en Chromium, escritorio y móvil | Revisada con una API simulada |
| Llamadas del frontend frente a rutas de la API | Revisadas: todas coinciden |
| Entidades frente a columnas SQL | Revisadas: todas coinciden |
| Paleta de gráficos: daltonismo y contraste | Validada |
| Compilación real con Maven | **Pendiente** |
| Pruebas de integración y concurrencia con MySQL | **Pendiente** |
| Generación real de PDF y Excel | **Pendiente** |
| Envío real de correo | **Pendiente** |
| Factiliza y PayPal con credenciales reales | **Pendiente** |
| Despliegue en Tomcat 10.1 | **Pendiente** |
| Fotografías de Wikimedia Commons: carga y licencia de cada archivo | **Pendiente** |

Al compilar por primera vez pueden aparecer errores que no se detectaron sin las librerías reales. Empieza por `mvn -DskipTests package` y luego `mvn test`.

## Tecnologías

Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Thymeleaf, MySQL 8, OpenHTMLtoPDF, Apache POI, Jakarta Mail, Maven y Tomcat 10.1.

Spring Boot 3.5 es la última línea compatible con Tomcat 10.1. Su soporte gratuito terminó el 30 de junio de 2026, así que un despliegue comercial debería migrar a Spring Boot 4 con Tomcat 11.

## Inicio rápido

```bash
mysql -u root -p < base-datos/esquema.sql
mysql -u root -p < base-datos/datos-iniciales.sql
cp .env.example .env
mvn spring-boot:run
```

Antes de arrancar, completa `BD_CONTRASENA`, `ADMIN_INICIAL_CONTRASENA` y `DEMO_CONTRASENA` en `.env`. Luego abre `http://localhost:8080`.

## Funciones

**Cliente:** catálogo con filtros, disponibilidad por fechas, registro con DNI, carrito, reserva en cinco etapas, pago, comprobantes y recuperación de contraseña.

**Trabajador:** pagos por verificar, llegadas, salidas e ingreso y salida de huéspedes.

**Administrador:** dashboard, habitaciones, categorías, clientes, usuarios, ventas, seis reportes con PDF y Excel, configuración y auditoría.

**Pagos:** efectivo, PayPal y Yape. PayPal y Yape aparecen cuando están configurados.

## Documentación

| Documento | Contenido |
|---|---|
| [Análisis y diseño](documentacion/01-analisis-y-diseno.md) | Requerimientos, reglas de negocio y diagramas |
| [Estructura y configuración](documentacion/02-estructura-y-configuracion.md) | Módulos, dependencias y variables |
| [Instalación](documentacion/03-instalacion.md) | MySQL, Tomcat, SMTP, Factiliza, PayPal y Yape |
| [Contratos de API](documentacion/04-contratos-api.md) | Rutas y formato de respuestas |
| [Protección contra DDoS](documentacion/05-proteccion-ddos.md) | Capas, límites y alcances |
| [Pruebas](documentacion/06-pruebas.md) | Matriz de cobertura |
| [Guion de sustentación](documentacion/07-guion-sustentacion.md) | Demostración de los seis hitos |
