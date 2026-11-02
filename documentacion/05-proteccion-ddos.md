# Protección contra ataques de denegación de servicio

## Qué protege y qué no

Ninguna aplicación puede frenar sola un ataque masivo. Cuando el tráfico satura la conexión del servidor, el daño ocurre antes de llegar a Java. Por eso la protección tiene tres capas.

| Capa | Dónde | Frena | Estado |
|---|---|---|---|
| 1. Borde | Cloudflare o similar | Ataques volumétricos | Requiere contratarlo y configurar el dominio |
| 2. Proxy | Nginx | Conexiones lentas y ráfagas | Plantilla en `nginx-hotel.conf` |
| 3. Aplicación | Filtros y límites propios | Abuso desde pocas IP y operaciones costosas | Incluida en el proyecto |

## Capa de aplicación incluida

| Medida | Clase o propiedad | Efecto |
|---|---|---|
| Límite por IP | `FiltroProteccionTrafico`, `ControlTrafico` | 300 solicitudes por minuto, 120 en la API. Responde `429` con `Retry-After` |
| Bloqueo temporal | `ControlTrafico` | Tres excesos bloquean la IP durante 10 minutos |
| Memoria acotada | `TRAFICO_MAXIMO_IPS` | Un ataque con muchas IP no agota la memoria del filtro |
| Agrupación IPv6 | `ControlTrafico.normalizarIp` | Trata un bloque `/64` como un solo origen |
| IP real tras un proxy | `ResolutorIpCliente` | Solo acepta `X-Forwarded-For` de proxies declarados |
| Inicio de sesión | `FiltroLimiteAcceso` | 30 intentos cada 10 minutos por IP |
| Consulta de DNI | `ServicioAutenticacion` | 5 por minuto y 20 por hora por IP, lo que también protege tu saldo |
| Códigos de recuperación | `ServicioRecuperacion` | Límites por IP y por correo |
| Exportaciones | `cupoExportaciones` | Máximo 4 simultáneas, luego responde `503` |
| Tomcat | `application.yml` | Hilos, conexiones, cabeceras y cuerpo limitados |
| Base de datos | Hikari | 12 conexiones y espera máxima de 5 segundos |
| Archivos | `spring.servlet.multipart` | Máximo 2 MB |

Los recursos estáticos no pasan por el límite: son baratos de servir y una página carga muchos a la vez.

## Ajustar los límites

```properties
TRAFICO_LIMITE_POR_MINUTO=300
TRAFICO_LIMITE_API_POR_MINUTO=120
TRAFICO_EXCESOS_PARA_BLOQUEAR=3
TRAFICO_MINUTOS_BLOQUEO=10
```

**Cuidado con una misma red:** en un laboratorio, todos los equipos pueden salir con la misma IP. Si la demostración recibe errores `429`, sube los límites o usa `TRAFICO_HABILITADO=false`.

Los contadores viven en memoria. Si despliegas varias instancias, cada una cuenta por separado y harían falta contadores compartidos, por ejemplo con Redis.

## Detrás de Nginx o Cloudflare

Declara la IP del proxy. Si no lo haces, todos los visitantes parecerán venir de la misma IP y se bloquearán entre sí.

```properties
TRAFICO_PROXIES_CONFIABLES=127.0.0.1
```

Con Cloudflare, publica tu servidor solo para sus rangos de IP. De lo contrario, un atacante que descubra tu IP puede saltarse la protección.

## Tomcat externo

Las propiedades `server.tomcat.*` solo aplican al Tomcat embebido. En un Tomcat instalado, ajusta el conector en `conf/server.xml`:

```xml
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="5000"
           maxThreads="100"
           maxConnections="2000"
           acceptCount="50"
           maxHttpHeaderSize="8192"
           maxKeepAliveRequests="100"
           keepAliveTimeout="15000"/>
```

## Plan de respuesta

1. Revisa los mensajes `Tráfico rechazado` en el registro de la aplicación.
2. Activa el modo de ataque de tu proveedor de borde.
3. Reduce temporalmente los límites de tráfico.
4. Si el ataque apunta a la ruta de catálogo, sirve esa ruta desde caché en el proxy.
