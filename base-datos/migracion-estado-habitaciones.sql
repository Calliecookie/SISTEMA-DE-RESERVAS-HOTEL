-- Migración para bases creadas con la versión anterior del esquema.
-- Cambia habitaciones.estado_operativo por habitaciones.estado (Disponible, Reservada, Ocupada,
-- En mantenimiento, Fuera de servicio). Las bases nuevas ya se crean así con esquema.sql.
SET NAMES utf8mb4;
USE hotel_reservas;

ALTER TABLE habitaciones DROP CHECK ck_habitaciones_estado;
ALTER TABLE habitaciones RENAME COLUMN estado_operativo TO estado;
UPDATE habitaciones SET estado = 'DISPONIBLE' WHERE estado = 'OPERATIVA';
ALTER TABLE habitaciones ALTER COLUMN estado SET DEFAULT 'DISPONIBLE';
ALTER TABLE habitaciones ADD CONSTRAINT ck_habitaciones_estado
  CHECK (estado IN ('DISPONIBLE', 'RESERVADA', 'OCUPADA', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO'));

-- Marca como Reservada u Ocupada las habitaciones que ya tienen reservas activas.
UPDATE habitaciones h
   SET h.estado = CASE
         WHEN EXISTS (SELECT 1 FROM detalle_reservas d JOIN reservas r ON r.id = d.reserva_id
                       WHERE d.habitacion_id = h.id AND r.estado = 'ENTREGADO') THEN 'OCUPADA'
         WHEN EXISTS (SELECT 1 FROM detalle_reservas d JOIN reservas r ON r.id = d.reserva_id
                       WHERE d.habitacion_id = h.id AND r.estado IN ('PAGADO', 'CONFIRMADO')
                         AND r.fecha_salida > CURDATE()) THEN 'RESERVADA'
         ELSE 'DISPONIBLE' END
 WHERE h.estado = 'DISPONIBLE';
