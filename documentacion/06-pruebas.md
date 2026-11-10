# Pruebas y control de calidad

## Estado actual

Las 125 pruebas (32 unitarias y 93 de integración contra MySQL 8) se ejecutaron y aprobaron el 10 de noviembre de 2026. Maven las corre en la zona horaria `America/Lima`, igual que la aplicación. Para la base de pruebas se usa `PRUEBA_BD_CONTRASENA` si tu usuario de MySQL tiene contraseña.

## Cómo ejecutarlas

Ver `03-instalacion.md`. Maven ejecuta todas las clases con sufijo `Prueba`.

## Matriz de cobertura

| Requisito del prompt | Clase | Escenarios |
|---|---|---|
| Registro de clientes | `AutenticacionPrueba` | Datos verificados, duplicados, contraseña débil |
| Validación de DNI | `AutenticacionPrueba` | Formato inválido, DNI inexistente |
| Consulta de Factiliza | `AutenticacionPrueba` | Respuesta correcta con token, fallo, límite de consultas |
| Inicio de sesión | `AutenticacionPrueba` | Redirección por rol, bloqueo, mensajes iguales |
| Recuperación de contraseña | `RecuperacionPrueba` | Flujo completo, reutilización, vencimiento, intentos, enumeración |
| Recuperación del administrador | `RecuperacionAdministradorPrueba` | Clave correcta, clave incorrecta, rol distinto, límite de intentos, auditoría |
| Protección de rutas | `SeguridadRutasPrueba` | Sin sesión, CSRF, cabeceras |
| Permisos por rol | `SeguridadRutasPrueba` | Matriz por rol, reservas ajenas |
| Registro de habitaciones | `AdministracionPrueba` | Ciclo completo, duplicados, imágenes |
| Gestión de categorías | `AdministracionPrueba` | Crear, duplicar, eliminar protegido |
| Búsqueda y filtros | `CatalogoCarritoPrueba` | Texto, comodines, precio, capacidad, orden |
| Cálculo de noches | `ValidadorEstanciaPrueba` | Cambio de mes y año, máximo |
| Cálculo de importes | `CalculadoraImportesPrueba`, `CatalogoCarritoPrueba` | IGV, descuento, precios manipulados |
| Prevención de duplicados | `ReservaPrueba` | Cuatro tipos de cruce y día de cambio |
| Confirmación de reservas | `ReservaPrueba` | Flujo completo de estados |
| Generación de PDF | `ReservaPrueba`, `AdministracionPrueba` | Comprobante y reportes |
| Envío de correo | `ReservaPrueba`, `RecuperacionPrueba` | SMTP real con GreenMail |
| Expiración automática | `ReservaPrueba` | Vencidas, disponibilidad liberada |
| CRUD administrativo | `AdministracionPrueba` | Usuarios, clientes, configuración |
| Reportes | `AdministracionPrueba` | Ingresos reales, seis reportes, exportación |
| Operaciones simultáneas | `ReservaPrueba` | 16 solicitudes por una habitación, orden cruzado |
| Pagos | `PagosPrueba` | Efectivo, Yape, PayPal, monto alterado, reembolso |
| Estado de habitaciones (en lugar de stock) | `EstadosPrueba` | Reservada al pagar, Ocupada al ingresar, Disponible al salir o cancelar, mantenimiento |
| Actualizar estado de reservas | `EstadosPrueba` | Pendiente → Pagado → Confirmado → Entregado → Finalizado, cancelación con motivo, transiciones inválidas, cliente sin permiso |
| Cierre del día | `EstadosPrueba` | Elimina pedidos no pagados, conserva los pagados, finaliza estancias vencidas |
| Cotizador, alternativas y calendario | `ConciergeMonitorPrueba` | Importes con IGV y descuento, límites, próxima fecha libre, afinidad, noches ocupadas |
| Mapa, planificador y búsqueda rápida | `ConciergeMonitorPrueba` | Situación por habitación, tramos por día, resultados por rol |
| Reportes de ocupación, métodos de pago y anticipación | `ConciergeMonitorPrueba` | Cifras, exportación a PDF y Excel, rango máximo |
| Total en letras | `ConversorNumeroLetrasPrueba`, `ReservaPrueba` | Reglas del castellano y texto dentro del PDF |
| Protección de tráfico | `ControlTraficoPrueba`, `ReglasSeguridadPrueba` | Límite, bloqueo, memoria, IP real |

Factiliza y PayPal se prueban contra un servidor HTTP local que imita sus respuestas documentadas. Esas pruebas validan la integración del sistema, no la conexión real con esos servicios. Para eso hacen falta tus credenciales.
