# Contratos entre frontend y backend

Toda respuesta JSON usa la misma estructura:

```json
{ "exito": true, "mensaje": "Operación realizada.", "datos": {}, "errores": null, "codigo": null }
```

| Campo | Uso |
|---|---|
| `exito` | `true` si la operación se completó |
| `mensaje` | Texto para mostrar al usuario |
| `datos` | Resultado de la operación |
| `errores` | Errores por campo, cuando falla la validación |
| `codigo` | Código estable del error, por ejemplo `HABITACION_OCUPADA` |

## Códigos HTTP

| Código | Significado |
|---|---|
| 200, 201 | Operación correcta |
| 400 | Datos inválidos |
| 401 | Falta iniciar sesión o las credenciales son incorrectas |
| 403 | Sin permiso o token CSRF inválido |
| 404 | No existe, o no pertenece al usuario |
| 409 | Conflicto de negocio, por ejemplo una habitación ya reservada |
| 413 | Archivo mayor a 2 MB |
| 429 | Demasiadas solicitudes |
| 503 | Servicio externo o exportación no disponible |

## Rutas

Las operaciones que cambian datos requieren el token CSRF en la cabecera `X-CSRF-TOKEN`.

| Método | Ruta | Operación | Acceso |
|---|---|---|---|
| GET | `/api/administracion/dashboard` | `dashboard` | Administrador |
| GET | `/api/administracion/habitaciones` | `habitaciones` | Administrador |
| GET | `/api/administracion/habitaciones/{id}` | `habitacion` | Administrador |
| GET | `/api/administracion/caracteristicas` | `caracteristicas` | Administrador |
| POST | `/api/administracion/habitaciones` | `crearHabitacion` | Administrador |
| PUT | `/api/administracion/habitaciones/{id}` | `actualizarHabitacion` | Administrador |
| PATCH | `/api/administracion/habitaciones/{id}/activo` | `cambiarActivoHabitacion` | Administrador |
| POST | `/api/administracion/habitaciones/{id}/imagen` | `subirImagen` | Administrador |
| DELETE | `/api/administracion/habitaciones/{id}` | `eliminarHabitacion` | Administrador |
| GET | `/api/administracion/categorias` | `categorias` | Administrador |
| GET | `/api/administracion/categorias/{id}` | `categoria` | Administrador |
| POST | `/api/administracion/categorias` | `crearCategoria` | Administrador |
| PUT | `/api/administracion/categorias/{id}` | `actualizarCategoria` | Administrador |
| PATCH | `/api/administracion/categorias/{id}/activo` | `cambiarActivoCategoria` | Administrador |
| DELETE | `/api/administracion/categorias/{id}` | `eliminarCategoria` | Administrador |
| GET | `/api/administracion/usuarios` | `usuarios` | Administrador |
| GET | `/api/administracion/usuarios/{id}` | `usuario` | Administrador |
| POST | `/api/administracion/usuarios` | `crearUsuario` | Administrador |
| PUT | `/api/administracion/usuarios/{id}` | `actualizarUsuario` | Administrador |
| PATCH | `/api/administracion/usuarios/{id}/activo` | `cambiarActivoUsuario` | Administrador |
| GET | `/api/administracion/usuarios/{id}/actividad` | `actividadUsuario` | Administrador |
| GET | `/api/administracion/clientes` | `clientes` | Administrador |
| PUT | `/api/administracion/clientes/{id}` | `actualizarCliente` | Administrador |
| PATCH | `/api/administracion/clientes/{id}/activo` | `cambiarActivoCliente` | Administrador |
| GET | `/api/administracion/ventas` | `ventas` | Administrador |
| GET | `/api/administracion/configuracion` | `configuracion` | Administrador |
| GET | `/api/administracion/configuracion/integraciones` | `integraciones` | Administrador |
| PUT | `/api/administracion/configuracion` | `actualizarConfiguracion` | Administrador |
| POST | `/api/administracion/configuracion/yape-qr` | `subirQrYape` | Administrador |
| GET | `/api/administracion/auditoria` | `auditoria` | Administrador |
| POST | `/api/autenticacion/consultar-dni` | `consultarDni` | Público |
| POST | `/api/autenticacion/registro` | `registrar` | Público |
| POST | `/api/autenticacion/recuperacion/solicitar` | `solicitarCodigo` | Público |
| POST | `/api/autenticacion/recuperacion/verificar` | `verificarCodigo` | Público |
| POST | `/api/autenticacion/recuperacion/restablecer` | `restablecer` | Público |
| GET | `/api/carrito` | `obtener` | Público |
| PUT | `/api/carrito/estancia` | `cambiarEstancia` | Público |
| POST | `/api/carrito/items` | `agregar` | Público |
| PATCH | `/api/carrito/items/{habitacionId}` | `cambiarHuespedes` | Público |
| DELETE | `/api/carrito/items/{habitacionId}` | `quitar` | Público |
| DELETE | `/api/carrito` | `vaciar` | Público |
| POST | `/api/cliente/reservas` | `crearReserva` | Cliente |
| GET | `/api/cliente/reservas` | `misReservas` | Cliente |
| GET | `/api/cliente/reservas/{codigo}` | `miReserva` | Cliente |
| GET | `/api/cliente/reservas/{codigo}/metodos-pago` | `metodosPago` | Cliente |
| POST | `/api/cliente/reservas/{codigo}/pago` | `declararPago` | Cliente |
| POST | `/api/cliente/reservas/{codigo}/paypal` | `iniciarPayPal` | Cliente |
| POST | `/api/cliente/reservas/{codigo}/cancelar` | `cancelar` | Cliente |
| GET | `/api/cliente/perfil` | `perfil` | Cliente |
| PUT | `/api/cliente/perfil` | `actualizarPerfil` | Cliente |
| PUT | `/api/cliente/contrasena` | `cambiarContrasena` | Cliente |
| GET | `/api/operaciones/resumen` | `resumen` | Trabajador, Administrador |
| GET | `/api/operaciones/reservas` | `reservas` | Trabajador, Administrador |
| GET | `/api/operaciones/reservas/{codigo}` | `reserva` | Trabajador, Administrador |
| POST | `/api/operaciones/reservas/{codigo}/confirmar-pago` | `confirmarPago` | Trabajador, Administrador |
| POST | `/api/operaciones/reservas/{codigo}/rechazar-pago` | `rechazarPago` | Trabajador, Administrador |
| POST | `/api/operaciones/reservas/{codigo}/confirmar` | `confirmar` | Trabajador, Administrador |
| POST | `/api/operaciones/reservas/{codigo}/ingreso` | `registrarIngreso` | Trabajador, Administrador |
| POST | `/api/operaciones/reservas/{codigo}/salida` | `registrarSalida` | Trabajador, Administrador |
| POST | `/api/operaciones/reservas/{codigo}/cancelar` | `cancelar` | Trabajador, Administrador |
| GET | `/api/publico/categorias` | `categorias` | Público |
| GET | `/api/publico/habitaciones` | `habitaciones` | Público |
| GET | `/api/publico/habitaciones/{id}` | `habitacion` | Público |
| GET | `/api/publico/habitaciones/{id}/disponibilidad` | `disponibilidad` | Público |
| GET | `/api/administracion/reportes/{clave}` | `reporte` | Administrador |
| GET | `/api/administracion/reportes/{clave}/exportar` | `exportar` | Administrador |
