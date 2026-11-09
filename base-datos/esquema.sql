SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS hotel_reservas
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE hotel_reservas;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS registros_auditoria;
DROP TABLE IF EXISTS historial_estados_reserva;
DROP TABLE IF EXISTS codigos_verificacion;
DROP TABLE IF EXISTS comprobantes;
DROP TABLE IF EXISTS pagos;
DROP TABLE IF EXISTS detalle_reservas;
DROP TABLE IF EXISTS reservas;
DROP TABLE IF EXISTS habitacion_caracteristicas;
DROP TABLE IF EXISTS caracteristicas;
DROP TABLE IF EXISTS habitaciones;
DROP TABLE IF EXISTS categorias;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS usuarios;
DROP TABLE IF EXISTS configuraciones;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE usuarios (
  id BIGINT NOT NULL AUTO_INCREMENT,
  correo VARCHAR(150) NOT NULL,
  contrasena_hash VARCHAR(255) NOT NULL,
  rol VARCHAR(20) NOT NULL,
  nombres VARCHAR(100) NOT NULL,
  apellidos VARCHAR(120) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  intentos_fallidos INT NOT NULL DEFAULT 0,
  bloqueado_hasta DATETIME NULL,
  ultimo_acceso DATETIME NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_usuarios_correo UNIQUE (correo),
  CONSTRAINT ck_usuarios_rol CHECK (rol IN ('CLIENTE', 'TRABAJADOR', 'ADMINISTRADOR')),
  CONSTRAINT ck_usuarios_intentos CHECK (intentos_fallidos >= 0),
  INDEX ix_usuarios_rol_activo (rol, activo)
) ENGINE = InnoDB;

CREATE TABLE clientes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  dni CHAR(8) NOT NULL,
  telefono VARCHAR(15) NULL,
  direccion VARCHAR(200) NULL,
  dni_verificado BOOLEAN NOT NULL DEFAULT FALSE,
  origen_datos VARCHAR(15) NOT NULL DEFAULT 'MANUAL',
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_clientes_usuario UNIQUE (usuario_id),
  CONSTRAINT uq_clientes_dni UNIQUE (dni),
  CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_clientes_dni CHECK (dni REGEXP '^[0-9]{8}$'),
  CONSTRAINT ck_clientes_origen CHECK (origen_datos IN ('FACTILIZA', 'MANUAL'))
) ENGINE = InnoDB;

CREATE TABLE categorias (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  descripcion VARCHAR(300) NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_categorias_nombre UNIQUE (nombre),
  INDEX ix_categorias_activo (activo)
) ENGINE = InnoDB;

CREATE TABLE caracteristicas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(80) NOT NULL,
  icono VARCHAR(40) NOT NULL DEFAULT 'check',
  PRIMARY KEY (id),
  CONSTRAINT uq_caracteristicas_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE habitaciones (
  id BIGINT NOT NULL AUTO_INCREMENT,
  numero VARCHAR(10) NOT NULL,
  nombre VARCHAR(120) NOT NULL,
  categoria_id BIGINT NOT NULL,
  descripcion VARCHAR(600) NOT NULL,
  capacidad INT NOT NULL,
  precio_noche DECIMAL(10, 2) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
  imagen_principal VARCHAR(500) NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_habitaciones_numero UNIQUE (numero),
  CONSTRAINT fk_habitaciones_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_habitaciones_capacidad CHECK (capacidad BETWEEN 1 AND 10),
  CONSTRAINT ck_habitaciones_precio CHECK (precio_noche > 0),
  CONSTRAINT ck_habitaciones_estado CHECK (estado IN ('DISPONIBLE', 'RESERVADA', 'OCUPADA', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO')),
  INDEX ix_habitaciones_catalogo (activo, estado, categoria_id),
  INDEX ix_habitaciones_precio (precio_noche)
) ENGINE = InnoDB;

CREATE TABLE habitacion_caracteristicas (
  habitacion_id BIGINT NOT NULL,
  caracteristica_id BIGINT NOT NULL,
  PRIMARY KEY (habitacion_id, caracteristica_id),
  CONSTRAINT fk_hc_habitacion FOREIGN KEY (habitacion_id) REFERENCES habitaciones (id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_hc_caracteristica FOREIGN KEY (caracteristica_id) REFERENCES caracteristicas (id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE reservas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  codigo VARCHAR(20) NOT NULL,
  cliente_id BIGINT NOT NULL,
  fecha_entrada DATE NOT NULL,
  fecha_salida DATE NOT NULL,
  cantidad_huespedes INT NOT NULL,
  subtotal DECIMAL(12, 2) NOT NULL,
  descuento DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
  impuesto DECIMAL(12, 2) NOT NULL,
  total DECIMAL(12, 2) NOT NULL,
  estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
  metodo_pago VARCHAR(20) NULL,
  fecha_pago DATETIME NULL,
  expira_en DATETIME NULL,
  observaciones VARCHAR(500) NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_reservas_codigo UNIQUE (codigo),
  CONSTRAINT fk_reservas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_reservas_fechas CHECK (fecha_salida > fecha_entrada),
  CONSTRAINT ck_reservas_huespedes CHECK (cantidad_huespedes >= 1),
  CONSTRAINT ck_reservas_importes CHECK (subtotal >= 0 AND descuento >= 0 AND impuesto >= 0 AND total >= 0),
  CONSTRAINT ck_reservas_estado CHECK (estado IN ('PENDIENTE', 'PAGADO', 'CONFIRMADO', 'ENTREGADO', 'FINALIZADO', 'CANCELADO', 'EXPIRADO')),
  CONSTRAINT ck_reservas_metodo CHECK (metodo_pago IS NULL OR metodo_pago IN ('EFECTIVO', 'PAYPAL', 'YAPE')),
  INDEX ix_reservas_disponibilidad (estado, fecha_entrada, fecha_salida),
  INDEX ix_reservas_cliente (cliente_id, creado_en),
  INDEX ix_reservas_expiracion (estado, expira_en),
  INDEX ix_reservas_creacion (creado_en)
) ENGINE = InnoDB;

CREATE TABLE detalle_reservas (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reserva_id BIGINT NOT NULL,
  habitacion_id BIGINT NOT NULL,
  cantidad_huespedes INT NOT NULL,
  precio_noche DECIMAL(10, 2) NOT NULL,
  noches INT NOT NULL,
  subtotal DECIMAL(12, 2) NOT NULL,
  descuento DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
  impuesto DECIMAL(12, 2) NOT NULL,
  total DECIMAL(12, 2) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_detalle_reserva_habitacion UNIQUE (reserva_id, habitacion_id),
  CONSTRAINT fk_detalle_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_detalle_habitacion FOREIGN KEY (habitacion_id) REFERENCES habitaciones (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_detalle_huespedes CHECK (cantidad_huespedes >= 1),
  CONSTRAINT ck_detalle_noches CHECK (noches >= 1),
  CONSTRAINT ck_detalle_importes CHECK (precio_noche > 0 AND subtotal >= 0 AND descuento >= 0 AND impuesto >= 0 AND total >= 0),
  INDEX ix_detalle_habitacion (habitacion_id, reserva_id)
) ENGINE = InnoDB;

CREATE TABLE pagos (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reserva_id BIGINT NOT NULL,
  metodo VARCHAR(20) NOT NULL,
  monto DECIMAL(12, 2) NOT NULL,
  numero_operacion VARCHAR(40) NULL,
  referencia_externa VARCHAR(60) NULL,
  moneda_externa CHAR(3) NULL,
  monto_externo DECIMAL(12, 2) NULL,
  tipo_cambio DECIMAL(10, 4) NULL,
  estado VARCHAR(15) NOT NULL DEFAULT 'DECLARADO',
  registrado_por BIGINT NOT NULL,
  confirmado_por BIGINT NULL,
  fecha_pago DATETIME NULL,
  motivo_rechazo VARCHAR(300) NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uq_pagos_referencia UNIQUE (referencia_externa),
  CONSTRAINT fk_pagos_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_pagos_registrado FOREIGN KEY (registrado_por) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_pagos_confirmado FOREIGN KEY (confirmado_por) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_pagos_monto CHECK (monto > 0),
  CONSTRAINT ck_pagos_metodo CHECK (metodo IN ('EFECTIVO', 'PAYPAL', 'YAPE')),
  CONSTRAINT ck_pagos_estado CHECK (estado IN ('INICIADO', 'DECLARADO', 'CONFIRMADO', 'RECHAZADO', 'REEMBOLSADO')),
  INDEX ix_pagos_ingresos (estado, fecha_pago),
  INDEX ix_pagos_reserva (reserva_id, estado),
  INDEX ix_pagos_operacion (metodo, numero_operacion)
) ENGINE = InnoDB;

CREATE TABLE comprobantes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reserva_id BIGINT NOT NULL,
  numero VARCHAR(40) NOT NULL,
  ruta_archivo VARCHAR(255) NOT NULL,
  estado_pago VARCHAR(30) NOT NULL,
  fecha_emision DATETIME NOT NULL,
  correo_destino VARCHAR(150) NOT NULL,
  correo_estado VARCHAR(15) NOT NULL DEFAULT 'PENDIENTE',
  correo_detalle VARCHAR(300) NULL,
  correo_fecha DATETIME NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_comprobantes_numero UNIQUE (numero),
  CONSTRAINT fk_comprobantes_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT ck_comprobantes_correo CHECK (correo_estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO', 'OMITIDO')),
  INDEX ix_comprobantes_reserva (reserva_id, fecha_emision)
) ENGINE = InnoDB;

CREATE TABLE codigos_verificacion (
  id BIGINT NOT NULL AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  proposito VARCHAR(30) NOT NULL,
  codigo_hash VARCHAR(255) NOT NULL,
  intentos INT NOT NULL DEFAULT 0,
  expira_en DATETIME NOT NULL,
  usado_en DATETIME NULL,
  invalidado_en DATETIME NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_codigos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT ck_codigos_proposito CHECK (proposito IN ('RECUPERAR_CONTRASENA')),
  CONSTRAINT ck_codigos_intentos CHECK (intentos >= 0),
  INDEX ix_codigos_vigentes (usuario_id, proposito, usado_en, invalidado_en)
) ENGINE = InnoDB;

CREATE TABLE historial_estados_reserva (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reserva_id BIGINT NOT NULL,
  estado_anterior VARCHAR(15) NULL,
  estado_nuevo VARCHAR(15) NOT NULL,
  usuario_id BIGINT NULL,
  actor VARCHAR(20) NOT NULL,
  motivo VARCHAR(300) NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_historial_reserva FOREIGN KEY (reserva_id) REFERENCES reservas (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT ck_historial_actor CHECK (actor IN ('CLIENTE', 'TRABAJADOR', 'ADMINISTRADOR', 'SISTEMA')),
  INDEX ix_historial_reserva (reserva_id, creado_en)
) ENGINE = InnoDB;

CREATE TABLE registros_auditoria (
  id BIGINT NOT NULL AUTO_INCREMENT,
  usuario_id BIGINT NULL,
  accion VARCHAR(60) NOT NULL,
  entidad VARCHAR(60) NOT NULL,
  entidad_id VARCHAR(40) NULL,
  detalle VARCHAR(500) NULL,
  direccion_ip VARCHAR(45) NULL,
  creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON UPDATE CASCADE ON DELETE SET NULL,
  INDEX ix_auditoria_fecha (creado_en),
  INDEX ix_auditoria_usuario (usuario_id, creado_en),
  INDEX ix_auditoria_entidad (entidad, entidad_id)
) ENGINE = InnoDB;

CREATE TABLE configuraciones (
  clave VARCHAR(60) NOT NULL,
  valor VARCHAR(300) NOT NULL,
  descripcion VARCHAR(200) NOT NULL,
  tipo VARCHAR(15) NOT NULL DEFAULT 'TEXTO',
  actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (clave),
  CONSTRAINT ck_configuraciones_tipo CHECK (tipo IN ('TEXTO', 'NUMERO', 'DECIMAL'))
) ENGINE = InnoDB;
