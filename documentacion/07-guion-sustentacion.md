# Guion de sustentación

Duración sugerida: 20 minutos.

## Preparación

1. Ejecuta `esquema.sql` y `datos-iniciales.sql`.
2. Activa `DEMO_HABILITADA=true` y arranca el sistema una vez.
3. Abre dos navegadores: uno como cliente y otro como administrador.
4. Las fotos del catálogo vienen incluidas en el proyecto, así que la demostración no depende de internet. Factiliza, PayPal y el correo sí lo necesitan.
5. Si la red es compartida, sube los límites de tráfico.

## Apertura (2 minutos)

Presenta el problema: una disponibilidad que depende de las fechas no se resuelve con un campo "disponible". Explica la arquitectura por capas y por qué se eligió Spring Boot 3.5: el laboratorio usa Tomcat 10.1.

## Primera revisión: base de datos y catálogo (3 minutos)

1. Muestra el diagrama entidad-relación y una restricción `CHECK`.
2. Abre `/habitaciones`.
3. Filtra por categoría, precio y capacidad.
4. Elige fechas y activa "Solo disponibles".

**Di:** "Una habitación ocupada del 10 al 13 está libre para entrar el 13, porque la salida no ocupa noche."

## Segunda revisión: carrito (3 minutos)

1. Añade dos habitaciones.
2. Cambia huéspedes y fechas.
3. Elige 5 noches para que aparezca el descuento.
4. Avanza por las cinco etapas.

**Di:** "El navegador nunca envía el precio. El servidor lo recalcula."

## Tercera revisión: confirmación y PDF (3 minutos)

1. Confirma la reserva.
2. Paga con Yape y escribe un número de operación.
3. Abre el comprobante y señala el total en letras al pie ("SON: … CON xx/100 SOLES").
4. Si configuraste SMTP, muestra el correo.
5. En *Habitaciones* del panel, muestra que la habitación pagada pasó de **Disponible** a **Reservada**: es el equivalente al stock que disminuye.

**Di:** "Los pedidos que no se pagan vencen a las 23:59 y el cierre del día los elimina."

**Di:** "El PDF y el correo se procesan después de confirmar la transacción. Si falla el correo, la reserva no se pierde."

**Pregunta probable:** ¿qué pasa si dos personas reservan a la vez? Bloqueo con `SELECT ... FOR UPDATE`, en orden, y verificación con `READ COMMITTED`. Con el nivel por defecto de MySQL, la segunda transacción podría leer una fotografía antigua y no ver la reserva recién confirmada.

## Cuarta revisión: administración (3 minutos)

1. Entra al dashboard.
2. Registra una habitación y sube una foto.
3. Intenta eliminar una habitación con reservas.
4. Crea un trabajador.
5. Abre la auditoría.
6. En *Reservas*, usa la columna **Actualizar estado** para pasar una reserva de Pagado a Confirmado y luego a Entregado. Muestra en la cuenta del cliente cómo avanza la barra de seguimiento y cómo la habitación pasa a Ocupada.
7. Cierra sesión y abre *Soy administrador y olvidé mi contraseña* en la pantalla de ingreso: código por correo o clave de recuperación (`ADMIN_CLAVE_RECUPERACION`).

## Quinta revisión: ventas (2 minutos)

1. Abre *Ventas* y filtra por método de pago.
2. Haz clic en una fila para abrir el detalle.
3. Confirma el pago de la reserva de la demostración.
4. Abre el comprobante actualizado.

## Sexta revisión: reportes (3 minutos)

1. Recorre los seis reportes.
2. Abre el historial de un cliente.
3. Descarga el PDF y el Excel.
4. Usa *Imprimir*.

**Di:** "Los ingresos se calculan con pagos confirmados. Las reservas pendientes, canceladas o reembolsadas no cuentan."

## Novedades de la versión 3.0 (2 minutos)

1. En el dashboard, muestra la tarjeta **Ventas efectuadas** y entra al detalle de una venta; luego la tarjeta **Reportes**.
2. Pulsa `Ctrl+K`, escribe un código de reserva y ábrelo con Enter.
3. Abre **Mapa de habitaciones**: filtra por "Ocupadas", pulsa una puerta y revisa el planificador.
4. En el sitio público, abre una habitación ocupada: elige fechas en el calendario y muestra la cotización con IGV, la próxima fecha libre y las alternativas.
5. En el catálogo, marca tres habitaciones y pulsa **Comparar**.
6. Cambia entre tema claro y oscuro.

## Seguridad (1 minuto)

Menciona Argon2id, CSRF, CSP, control por rol, validación de propiedad, límites de intentos y las tres capas contra DDoS. Aclara que la capa de aplicación no detiene un ataque masivo.

## Preguntas probables

| Pregunta | Respuesta |
|---|---|
| ¿Por qué no Dompdf ni PHPMailer? | El sílabo pide Java. Se usaron OpenHTMLtoPDF y Spring Mail |
| ¿Es una boleta electrónica? | No. Es un comprobante de reserva, y emitir boletas requiere un proveedor autorizado |
| ¿Por qué PayPal cobra en dólares? | PayPal no admite soles |
| ¿Por qué se verifica Yape a mano? | No encontramos una API pública oficial para comercios |
| ¿Por qué no se usa Spring Boot 4? | Exige Servlet 6.1, que Tomcat 10.1 no ofrece |
| ¿Cómo se evita ver reservas ajenas? | El servicio compara al dueño y responde 404 |
