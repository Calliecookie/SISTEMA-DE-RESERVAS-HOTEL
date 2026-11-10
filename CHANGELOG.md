# Registro de cambios

Todas las versiones del Sistema de Reservas Hotel Inti Wasi. El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y la numeración [Versionado Semántico](https://semver.org/lang/es/).

**Convención de versiones**

- Un cambio grande (nuevos módulos, cambios en la base de datos o en el flujo principal) sube la versión mayor: 2.0 → **3.0**.
- Un cambio pequeño (ajustes, correcciones, mejoras puntuales) sube la versión menor: 2.0 → **2.1**.
- Cada versión se trabaja en una rama con el número y un título que resume la mejora, por ejemplo `v2.1-filtro-de-reservas-por-habitacion`.
- Cada versión queda en su propia rama, independiente: `main` conserva la versión 1.0 y las versiones nuevas no se unen a `main`. Una versión nueva parte de la rama de la versión anterior.

## [3.0.0] - 2026-11-10

Versión grande: agrega el concierge para el cliente, el monitor en vivo para el personal y tres reportes de gestión hotelera. Esta entrada cubre el servidor (Java, SQL y plantillas); la interfaz que usa estos servicios se incorpora en la misma versión.

### Novedades

- **Cotizador y comparador de habitaciones.** `GET /api/publico/cotizacion` calcula, para hasta cuatro habitaciones y un rango de fechas, el subtotal, el descuento por estadía larga, el IGV y el total por habitación. Indica el precio promedio por noche, cuántas noches faltan para obtener el descuento y cuánto se ahorraría.
- **Concierge de alternativas.** `GET /api/publico/habitaciones/{id}/sugerencias` responde, cuando una habitación no está libre, la próxima fecha en que sí lo estará para la misma cantidad de noches y hasta tres habitaciones disponibles ordenadas por afinidad (categoría, capacidad, precio y servicios), con los motivos de cada sugerencia.
- **Calendario de disponibilidad.** `GET /api/publico/habitaciones/{id}/calendario` devuelve día por día si la habitación está libre.
- **Mapa de habitaciones en vivo.** `GET /api/operaciones/mapa` agrupa las habitaciones por piso con su situación actual (Disponible, Reservada, Ocupada, Reserva pendiente, En mantenimiento, Fuera de servicio), el huésped, las fechas y las llegadas y salidas del día.
- **Planificador de ocupación.** `GET /api/operaciones/planificador` entrega la grilla de habitaciones por días con cada reserva ubicada en su tramo y la ocupación diaria.
- **Búsqueda rápida del panel.** `GET /api/operaciones/buscar` encuentra reservas, habitaciones y clientes para la paleta de comandos. Los clientes solo se muestran al administrador.
- **Tres reportes nuevos**, exportables a PDF y Excel como los demás: Ocupación, tarifa promedio (ADR) y RevPAR; Ingresos por método de pago; Anticipación de la reserva. El centro de reportes pasa de seis a nueve.
- **Dashboard ampliado.** Ventas efectuadas (total y del mes), ticket promedio, ingresos del mes anterior, ocupación de hoy, tarifa promedio y RevPAR de los últimos 30 días, pagos por verificar, llegadas y salidas pendientes, pronóstico de ocupación a 14 días y cantidad de habitaciones por estado.

### Cambios

- La boleta PDF tiene un nuevo diseño: franja institucional, sello con el estado del pago, forma y fecha de pago, y el total en letras resaltado.
- Los correos (boleta, código de recuperación y cambio de estado) comparten una cabecera con la identidad del hotel.
- `Formato.importeEnLetras` expone el conversor de importes a letras.
- El código fuente queda sin comentarios.

### Correcciones

- Las pruebas se ejecutan en la zona horaria `America/Lima`, la misma que usa la aplicación. Antes, al correr en UTC, las fechas se guardaban con un día de diferencia solo durante las pruebas.

### Pruebas

- 125 pruebas aprobadas (32 unitarias y 93 de integración contra MySQL 8). `ConciergeMonitorPrueba` agrega 9: cotización, límites, sugerencias, calendario, mapa, planificador, búsqueda por rol, dashboard y reportes nuevos con sus exportaciones.

## [2.0.0] - 2026-11-09

Versión que atiende las observaciones de la revisión de los hitos y completa los requerimientos pendientes.

### Novedades

- **Estado de la habitación en lugar de stock** (observación 14). La tabla `habitaciones` tiene el campo `estado` con los valores Disponible, Reservada, Ocupada, En mantenimiento y Fuera de servicio. El sistema lo actualiza solo:
  - al pagarse una reserva, la habitación pasa a **Reservada** (equivale al stock que disminuye);
  - al registrar el ingreso del huésped, pasa a **Ocupada**;
  - al registrar la salida o cancelar la reserva, vuelve a **Disponible**.

  El administrador solo fija a mano Disponible, En mantenimiento o Fuera de servicio. La pantalla *Habitaciones* del panel tiene un filtro por estado y un resumen con la cantidad de habitaciones en cada estado. El catálogo y el reporte "Estado de habitaciones y disponibilidad" muestran el nuevo estado.
- **Recuperación de la contraseña del administrador** (observación 15). Página `/recuperar-contrasena/administrador`, enlazada desde la pantalla de ingreso, con dos métodos:
  - código de 6 dígitos por correo;
  - clave de recuperación `ADMIN_CLAVE_RECUPERACION` del `.env`, para cuando el correo no está configurado.

  Tiene límite de intentos, registra en la auditoría los intentos fallidos y el restablecimiento, y desbloquea la cuenta.
- **Actualizar el estado de las reservas de los clientes** (observación 15). La pantalla *Reservas* del panel tiene la columna **Actualizar estado** para pasar una reserva de Pendiente a Pagado, Confirmado, Entregado y Finalizado, o cancelarla con un motivo. Nueva ruta `PATCH /api/operaciones/reservas/{codigo}/estado`.
- **Seguimiento de la reserva para el cliente.** El detalle de la reserva muestra una barra con el avance: Pendiente de pago → Pagado → Confirmado → Entregado → Finalizado.
- **Aviso por correo al cliente** cada vez que cambia el estado de su reserva (si el correo está configurado).
- **Total en letras en la boleta.** El comprobante PDF muestra al pie el importe en letras, por ejemplo: "SON: MIL VEINTISÉIS CON 60/100 SOLES".
- **Cierre del día.** A las 23:59 una tarea automática:
  - da de baja y elimina los pedidos que el cliente no pagó, con sus detalles y comprobantes;
  - finaliza las estancias cuya fecha de salida ya pasó;
  - recalcula el estado de las habitaciones.

  Los pedidos con un pago declarado por verificar no se eliminan. Se configura con `TAREA_CIERRE_DIA_CRON` y `TAREA_ELIMINAR_NO_PAGADAS`.

### Cambios

- El plazo de pago de una reserva pendiente vence, como máximo, a las 23:59 del día en que se hizo.
- La salida de un huésped (Entregado → Finalizado) también la puede registrar el sistema en el cierre del día.
- El listado de reservas del personal incluye `estadosSiguientes` con los estados a los que puede pasar cada reserva.
- El reporte "Stock y disponibilidad" ahora se llama "Estado de habitaciones y disponibilidad".
- Nuevas variables en `.env.example`: `ADMIN_CLAVE_RECUPERACION`, `TAREA_CIERRE_DIA_CRON` y `TAREA_ELIMINAR_NO_PAGADAS`.

### Correcciones

- **Imágenes del catálogo** (observación 14). Las 12 fotografías con licencia CC citadas en `/creditos` no estaban en el repositorio y el catálogo se mostraba sin imágenes. Se agregaron en `recursos/imagenes/habitaciones/`.
- **Las pruebas no compilaban.** Faltaba la clase `CargadorEsquemaPrueba`, que crea las tablas de la base de pruebas. Se restauró.
- El estado de las habitaciones se recalcula también al crear los datos de demostración, para que la demo arranque con habitaciones Reservadas y Ocupadas coherentes con sus reservas.

### Base de datos

- `habitaciones.estado_operativo` se renombró a `habitaciones.estado` con los nuevos valores permitidos.
- Para una base creada con la versión 1.0.0, ejecuta una vez:

  ```bash
  mysql -u root -p < base-datos/migracion-estado-habitaciones.sql
  ```

  Las instalaciones nuevas solo necesitan `esquema.sql` y `datos-iniciales.sql`.

### Pruebas

- 116 pruebas aprobadas (32 unitarias y 84 de integración contra MySQL 8). Se agregaron 13:
  - `EstadosPrueba`: estado de habitaciones, cambios de estado de reservas, permisos, cierre del día y plazo de pago;
  - `RecuperacionAdministradorPrueba`: clave correcta e incorrecta, rol distinto, límite de intentos y auditoría;
  - `ConversorNumeroLetrasPrueba` y una verificación en `ReservaPrueba` que lee el total en letras dentro del PDF.

## [1.0.0] - 2026-11-02

Primera versión: catálogo con filtros y disponibilidad por fechas, registro con DNI, carrito y reserva en cinco etapas, pagos en efectivo, PayPal y Yape, comprobante PDF por correo, panel del trabajador, panel del administrador con CRUD, ventas, seis reportes en PDF y Excel, configuración y auditoría.

[3.0.0]: https://github.com/Calliecookie/SISTEMA-DE-RESERVAS-HOTEL/tree/v3.0-experiencia-andina-mapa-de-habitaciones-y-concierge
[2.0.0]: https://github.com/Calliecookie/SISTEMA-DE-RESERVAS-HOTEL/tree/v2.0-estado-de-habitaciones-y-gestion-de-reservas
[1.0.0]: https://github.com/Calliecookie/SISTEMA-DE-RESERVAS-HOTEL/tree/e5a5986
