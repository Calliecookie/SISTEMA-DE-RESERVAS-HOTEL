# Pruebas y control de calidad

## Estado actual

**Las pruebas de integración aún no se han ejecutado.** Se escribieron sin acceso a Maven Central, MySQL ni al computador del autor. Consulta la sección "Estado de verificación" del `README.md`.

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
| Protección de tráfico | `ControlTraficoPrueba`, `ReglasSeguridadPrueba` | Límite, bloqueo, memoria, IP real |

Factiliza y PayPal se prueban contra un servidor HTTP local que imita sus respuestas documentadas. Esas pruebas validan la integración del sistema, no la conexión real con esos servicios. Para eso hacen falta tus credenciales.
