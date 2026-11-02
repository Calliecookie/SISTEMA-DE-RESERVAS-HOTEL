USE hotel_reservas;

INSERT INTO configuraciones (clave, valor, descripcion, tipo) VALUES
  ('hotel_nombre', 'Hotel Inti Wasi', 'Nombre del hotel', 'TEXTO'),
  ('hotel_ruc', '20000000000', 'RUC del establecimiento', 'TEXTO'),
  ('hotel_direccion', 'Dirección de ejemplo, Lima, Perú', 'Dirección del hotel', 'TEXTO'),
  ('hotel_telefono', '+51 900 000 000', 'Teléfono de reservas', 'TEXTO'),
  ('hotel_correo', 'reservas@ejemplo.pe', 'Correo de reservas', 'TEXTO'),
  ('impuesto_porcentaje', '18.00', 'Porcentaje de IGV', 'DECIMAL'),
  ('horas_expiracion_pendiente', '24', 'Horas para pagar una reserva pendiente', 'NUMERO'),
  ('horas_verificacion_pago', '12', 'Horas para verificar un pago declarado', 'NUMERO'),
  ('descuento_noches_minimas', '5', 'Noches mínimas para el descuento por estadía larga', 'NUMERO'),
  ('descuento_porcentaje', '5.00', 'Porcentaje de descuento por estadía larga', 'DECIMAL'),
  ('paypal_tipo_cambio', '', 'Tipo de cambio para PayPal (soles por dólar)', 'DECIMAL'),
  ('yape_numero', '', 'Número de celular de Yape', 'TEXTO'),
  ('yape_titular', '', 'Titular de la cuenta de Yape', 'TEXTO'),
  ('yape_qr', '', 'Imagen del código QR de Yape', 'TEXTO');

INSERT INTO categorias (id, nombre, descripcion) VALUES
  (1, 'Estándar', 'Habitaciones cómodas y funcionales para estancias cortas.'),
  (2, 'Superior', 'Más espacio y mejores vistas para descansar con calma.'),
  (3, 'Matrimonial', 'Cama king y detalles pensados para dos personas.'),
  (4, 'Familiar', 'Ambientes amplios para viajar con niños o en grupo.'),
  (5, 'Suite', 'Sala independiente y los mejores acabados del hotel.');

INSERT INTO caracteristicas (id, nombre, icono) VALUES
  (1, 'Wi-Fi de alta velocidad', 'wifi'),
  (2, 'Aire acondicionado', 'aire'),
  (3, 'Desayuno incluido', 'cafe'),
  (4, 'Smart TV', 'tv'),
  (5, 'Baño privado', 'bano'),
  (6, 'Caja fuerte', 'caja'),
  (7, 'Minibar', 'vino'),
  (8, 'Vista a la ciudad', 'ciudad'),
  (9, 'Balcón', 'balcon'),
  (10, 'Tina de hidromasaje', 'ondas'),
  (11, 'Escritorio de trabajo', 'laptop'),
  (12, 'Cuna disponible', 'bebe'),
  (13, 'Servicio a la habitación', 'conserje'),
  (14, 'Calefacción', 'termometro');

INSERT INTO habitaciones (id, numero, nombre, categoria_id, descripcion, capacidad, precio_noche, estado_operativo, imagen_principal) VALUES
  (1, '101', 'Estándar Patio', 1, 'Habitación tranquila con vista al patio interior, ideal para viajes de trabajo o escapadas cortas.', 2, 150.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Room_in_Hotel.jpg?width=960'),
  (2, '102', 'Estándar Jardín', 1, 'Luz natural durante toda la mañana y acceso directo al jardín del primer piso.', 2, 165.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/UMass_Hotel_room.JPG?width=960'),
  (3, '103', 'Estándar Individual', 1, 'Pensada para una persona que viaja ligero: cama amplia, escritorio y todo a mano.', 1, 120.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Hotel_Room_with_queen_size_bed.jpg?width=960'),
  (4, '201', 'Superior Ciudad', 2, 'Segundo piso con vista a la ciudad, escritorio amplio y mayor aislamiento acústico.', 2, 220.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Hotel_room_interior_at_hotel_Radisson_Blu_Oulu.jpg?width=960'),
  (5, '202', 'Superior Balcón', 2, 'Balcón privado para tomar el desayuno al aire libre y ver caer la tarde.', 3, 260.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Bedroom_hotel_interior_with_open_door_window._%2851536308276%29.jpg?width=960'),
  (6, '203', 'Superior Ejecutiva', 2, 'Mesa de trabajo, sillón de lectura y conexión estable para quienes viajan por negocios.', 2, 240.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Mitsui_Garden_Hotel_Kanazawa_Moderate_Double_bedroom_20200103-001.jpg?width=960'),
  (7, '301', 'Matrimonial Sol', 3, 'Cama king orientada al amanecer, con detalles de bienvenida para celebrar en pareja.', 2, 290.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Hotel_room_at_Brahe_Ensuite.jpg?width=960'),
  (8, '302', 'Matrimonial Terraza', 3, 'Terraza privada y tina de hidromasaje para una estancia sin prisas.', 2, 340.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Hotel_room_interior_at_apartment_hotel_Ona_Cala_P%C3%AD_Club%2C_Mallorca%2C_Spain.jpg?width=960'),
  (9, '401', 'Familiar Andina', 4, 'Dos ambientes conectados, camas para cuatro personas y cuna a pedido.', 4, 380.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Bridal_Tea_House_Hotel_-_Modern_Family_Room.jpg?width=960'),
  (10, '402', 'Familiar Grande', 4, 'Nuestra habitación más amplia para grupos: seis camas y dos baños completos.', 6, 520.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Bedroom_Efteling_Loonsche_Land_Hotel.jpg?width=960'),
  (11, '501', 'Suite Inti', 5, 'Sala independiente, tina de hidromasaje y la mejor vista del hotel.', 3, 620.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Bedroom%27s_Pardoes_suite_Efteling_hotel.jpg?width=960'),
  (12, '502', 'Suite Wasi', 5, 'Dos dormitorios, comedor privado y atención personalizada durante toda la estancia.', 5, 780.00, 'OPERATIVA', 'https://commons.wikimedia.org/wiki/Special:FilePath/Living_room%27s_Pardoes_suite_Efteling_hotel.jpg?width=960'),
  (13, '104', 'Estándar Claustro', 1, 'Habitación en mantenimiento programado. Volverá a estar disponible pronto.', 2, 150.00, 'MANTENIMIENTO', 'https://commons.wikimedia.org/wiki/Special:FilePath/Room_in_Hotel.jpg?width=960');

INSERT INTO habitacion_caracteristicas (habitacion_id, caracteristica_id) VALUES
  (1, 1), (1, 3), (1, 4), (1, 5),
  (2, 1), (2, 3), (2, 4), (2, 5), (2, 14),
  (3, 1), (3, 3), (3, 5), (3, 11),
  (4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 8),
  (5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 9),
  (6, 1), (6, 2), (6, 3), (6, 4), (6, 5), (6, 11),
  (7, 1), (7, 2), (7, 3), (7, 4), (7, 5), (7, 7), (7, 13),
  (8, 1), (8, 2), (8, 3), (8, 4), (8, 5), (8, 7), (8, 9), (8, 10),
  (9, 1), (9, 2), (9, 3), (9, 4), (9, 5), (9, 12),
  (10, 1), (10, 2), (10, 3), (10, 4), (10, 5), (10, 12), (10, 14),
  (11, 1), (11, 2), (11, 3), (11, 4), (11, 5), (11, 6), (11, 7), (11, 8), (11, 10), (11, 13),
  (12, 1), (12, 2), (12, 3), (12, 4), (12, 5), (12, 6), (12, 7), (12, 8), (12, 13), (12, 14),
  (13, 1), (13, 3), (13, 4), (13, 5);
