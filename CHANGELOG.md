# Registro de cambios

Todas las versiones del Sistema de Reservas Hotel Inti Wasi. El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y la numeración [Versionado Semántico](https://semver.org/lang/es/).

**Convención de versiones**

- Un cambio grande (nuevos módulos, cambios en la base de datos o en el flujo principal) sube la versión mayor: 2.0 → **3.0**.
- Un cambio pequeño (ajustes, correcciones, mejoras puntuales) sube la versión menor: 2.0 → **2.1**.
- Cada versión se trabaja en una rama con el número y un título que resume la mejora, por ejemplo `v2.1-filtro-de-reservas-por-habitacion`.

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

[2.0.0]: https://github.com/Calliecookie/SISTEMA-DE-RESERVAS-HOTEL/releases/tag/v2.0.0
[1.0.0]: https://github.com/Calliecookie/SISTEMA-DE-RESERVAS-HOTEL/tree/e5a5986
