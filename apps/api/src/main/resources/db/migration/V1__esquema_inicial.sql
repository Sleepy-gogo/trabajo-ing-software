CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuarios (
    id UUID NOT NULL,
    nombre_completo VARCHAR(200) NOT NULL,
    email VARCHAR(100) NOT NULL,
    dni INTEGER NOT NULL,
    estado_cuenta VARCHAR(20) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    qr_usuario VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_usuarios_qr_formato CHECK (((qr_usuario)::TEXT ~ '^SERA-U[a-z0-9]{12}$'::TEXT)),
    CONSTRAINT ck_usuarios_rol CHECK ((rol IN ('ADMIN', 'STAFF', 'USUARIO'))),
    CONSTRAINT usuarios_dni_check CHECK (((dni >= 1) AND (dni <= 99999999))),
    CONSTRAINT usuarios_estado_cuenta_check CHECK ((estado_cuenta IN ('ACTIVO', 'DESHABILITADO', 'INACTIVO'))),
    CONSTRAINT uk_usuarios_dni UNIQUE (dni),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_qr_usuario UNIQUE (qr_usuario),
    CONSTRAINT usuarios_pkey PRIMARY KEY (id)
);

CREATE TABLE espacios (
    id UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    capacidad INTEGER NOT NULL,
    tarifa_hora NUMERIC(12,2) NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    ruta_imagen VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado VARCHAR(30) DEFAULT 'HABILITADO'::VARCHAR NOT NULL,
    CONSTRAINT ck_espacio_estado CHECK ((estado IN ('HABILITADO', 'MANTENIMIENTO', 'INUTILIZABLE', 'EN_USO'))),
    CONSTRAINT ck_espacios_capacidad_positiva CHECK ((capacidad > 0)),
    CONSTRAINT ck_espacios_tarifa_no_negativa CHECK ((tarifa_hora >= (0)::NUMERIC)),
    CONSTRAINT espacios_pkey PRIMARY KEY (id)
);

CREATE TABLE nivel_membresias (
    id UUID NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    descripcion VARCHAR(500),
    disponible BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT nivel_membresias_nombre_key UNIQUE (nombre),
    CONSTRAINT nivel_membresias_pkey PRIMARY KEY (id)
);

CREATE TABLE nivel_membresia_precio_relacion (
    nivel_membresia_id UUID NOT NULL,
    relacion VARCHAR(50) NOT NULL,
    precio NUMERIC(10,2) NOT NULL,
    CONSTRAINT ck_precio_membresia_positivo CHECK ((precio > (0)::NUMERIC)),
    CONSTRAINT nivel_membresia_precio_relacion_pkey PRIMARY KEY (nivel_membresia_id, relacion),
    CONSTRAINT fk_precio_nivel FOREIGN KEY (nivel_membresia_id) REFERENCES nivel_membresias(id) ON DELETE CASCADE
);

CREATE TABLE nivel_membresia_beneficios (
    nivel_membresia_id UUID NOT NULL,
    beneficio VARCHAR(255) NOT NULL,
    CONSTRAINT fk_beneficio_nivel FOREIGN KEY (nivel_membresia_id) REFERENCES nivel_membresias(id) ON DELETE CASCADE
);

CREATE TABLE socios (
    id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    relacion_unse VARCHAR(50) NOT NULL,
    estado_verificacion_unse VARCHAR(50) NOT NULL,
    identificador_unse VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT socios_identificador_unse_key UNIQUE (identificador_unse),
    CONSTRAINT socios_pkey PRIMARY KEY (id),
    CONSTRAINT socios_usuario_id_key UNIQUE (usuario_id),
    CONSTRAINT fk_socio_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE membresias (
    id UUID NOT NULL,
    socio_id UUID NOT NULL,
    nivel_membresia_id UUID NOT NULL,
    estado VARCHAR(50) NOT NULL,
    fecha_alta DATE NOT NULL,
    fecha_baja DATE,
    proximo_vencimiento DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    contratacion_id UUID NOT NULL,
    CONSTRAINT ck_membresias_estado CHECK ((estado IN ('PENDIENTE_PAGO', 'ACTIVA', 'VENCIDA', 'SUSPENDIDA', 'CANCELADA'))),
    CONSTRAINT membresias_pkey PRIMARY KEY (id),
    CONSTRAINT membresias_socio_id_key UNIQUE (socio_id),
    CONSTRAINT fk_membresia_nivel FOREIGN KEY (nivel_membresia_id) REFERENCES nivel_membresias(id),
    CONSTRAINT fk_membresia_socio FOREIGN KEY (socio_id) REFERENCES socios(id)
);

CREATE TABLE cambios_socios (
    id UUID NOT NULL,
    socio_id UUID NOT NULL,
    responsable_id UUID NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    detalle VARCHAR(2000) NOT NULL,
    fecha TIMESTAMPTZ NOT NULL,
    CONSTRAINT cambios_socios_pkey PRIMARY KEY (id),
    CONSTRAINT cambios_socios_responsable_id_fkey FOREIGN KEY (responsable_id) REFERENCES usuarios(id),
    CONSTRAINT cambios_socios_socio_id_fkey FOREIGN KEY (socio_id) REFERENCES socios(id)
);

CREATE TABLE disponibilidades (
    id UUID NOT NULL,
    id_espacio UUID NOT NULL,
    dia_semana VARCHAR(10) NOT NULL,
    hora_desde TIME NOT NULL,
    hora_hasta TIME NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_disponibilidades_dia_semana CHECK ((dia_semana IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO'))),
    CONSTRAINT ck_disponibilidades_rango CHECK ((hora_desde < hora_hasta)),
    CONSTRAINT disponibilidades_pkey PRIMARY KEY (id),
    CONSTRAINT fk_disponibilidades_espacio FOREIGN KEY (id_espacio) REFERENCES espacios(id) ON DELETE CASCADE
);

CREATE TABLE espacio_tarifas (
    espacio_id UUID NOT NULL,
    relacion VARCHAR(30) NOT NULL,
    importe NUMERIC(12,2) NOT NULL,
    CONSTRAINT espacio_tarifas_importe_check CHECK ((importe >= (0)::NUMERIC)),
    CONSTRAINT espacio_tarifas_pkey PRIMARY KEY (espacio_id, relacion),
    CONSTRAINT espacio_tarifas_espacio_id_fkey FOREIGN KEY (espacio_id) REFERENCES espacios(id)
);

CREATE TABLE bloqueos_espacios (
    id UUID NOT NULL,
    espacio_id UUID NOT NULL,
    fecha DATE NOT NULL,
    desde TIME NOT NULL,
    hasta TIME NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    CONSTRAINT ck_bloqueo_horario CHECK ((desde < hasta)),
    CONSTRAINT bloqueos_espacios_pkey PRIMARY KEY (id),
    CONSTRAINT bloqueos_espacios_espacio_id_fkey FOREIGN KEY (espacio_id) REFERENCES espacios(id)
);

CREATE TABLE reservas (
    id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    espacio_id UUID NOT NULL,
    fecha DATE NOT NULL,
    desde TIME NOT NULL,
    hasta TIME NOT NULL,
    personas INTEGER NOT NULL,
    tarifa_hora NUMERIC(12,2) NOT NULL,
    relacion_aplicada VARCHAR(30) NOT NULL,
    total NUMERIC(12,2) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    codigo VARCHAR(50),
    vence_en TIMESTAMPTZ NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL,
    ticket_origen_id UUID,
    credito_aplicado NUMERIC(12,2) DEFAULT 0 NOT NULL,
    saldo_ticket NUMERIC(12,2) DEFAULT 0 NOT NULL,
    clave_solicitud UUID NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    consumida_en TIMESTAMPTZ,
    consumida_por UUID,
    CONSTRAINT ck_reserva_consumida CHECK ((((consumida_en IS NULL) AND (consumida_por IS NULL)) OR ((consumida_en IS NOT NULL) AND (consumida_por IS NOT NULL) AND ((estado)::TEXT = 'CONFIRMADA'::TEXT)))),
    CONSTRAINT reservas_check CHECK (((credito_aplicado >= (0)::NUMERIC) AND (credito_aplicado <= total))),
    CONSTRAINT reservas_check1 CHECK ((desde < hasta)),
    CONSTRAINT reservas_estado_check CHECK ((estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'CANCELADA', 'VENCIDA'))),
    CONSTRAINT reservas_personas_check CHECK ((personas > 0)),
    CONSTRAINT reservas_saldo_ticket_check CHECK ((saldo_ticket >= (0)::NUMERIC)),
    CONSTRAINT reservas_tarifa_hora_check CHECK ((tarifa_hora >= (0)::NUMERIC)),
    CONSTRAINT reservas_total_check CHECK ((total >= (0)::NUMERIC)),
    CONSTRAINT reservas_codigo_key UNIQUE (codigo),
    CONSTRAINT reservas_pkey PRIMARY KEY (id),
    CONSTRAINT reservas_sin_superposicion EXCLUDE USING gist (espacio_id WITH =, tsrange((fecha + desde), (fecha + hasta), '[)'::TEXT) WITH &&) WHERE ((estado IN ('PENDIENTE_PAGO', 'CONFIRMADA'))),
    CONSTRAINT reservas_usuario_id_clave_solicitud_key UNIQUE (usuario_id, clave_solicitud),
    CONSTRAINT reservas_consumida_por_fkey FOREIGN KEY (consumida_por) REFERENCES usuarios(id),
    CONSTRAINT reservas_espacio_id_fkey FOREIGN KEY (espacio_id) REFERENCES espacios(id),
    CONSTRAINT reservas_ticket_origen_id_fkey FOREIGN KEY (ticket_origen_id) REFERENCES reservas(id),
    CONSTRAINT reservas_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE pagos (
    id UUID NOT NULL,
    concepto VARCHAR(100) NOT NULL,
    usuario_id UUID NOT NULL,
    estado VARCHAR(50) NOT NULL,
    medio_pago VARCHAR(50) NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    comprobante VARCHAR(255),
    membresia_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL,
    mercado_pago_payment_id VARCHAR(100),
    contratacion_id UUID,
    clave_solicitud UUID NOT NULL,
    relacion_aplicada VARCHAR(30) NOT NULL,
    nivel_nombre_aplicado VARCHAR(255) NOT NULL,
    aprobado_en TIMESTAMPTZ,
    mercado_pago_factura_id BIGINT,
    aplicado_en TIMESTAMPTZ,
    vencimiento_anterior DATE,
    vencimiento_resultante DATE,
    requiere_revision BOOLEAN DEFAULT false NOT NULL,
    motivo_revision VARCHAR(500),
    reserva_id UUID,
    checkout_url TEXT,
    CONSTRAINT ck_pago_reserva CHECK (((reserva_id IS NULL) OR ((membresia_id IS NULL) AND (concepto IN ('RESERVA', 'DIFERENCIA_TICKET'))))),
    CONSTRAINT ck_pagos_monto_positivo CHECK ((monto > (0)::NUMERIC)),
    CONSTRAINT pagos_mercado_pago_payment_id_key UNIQUE (mercado_pago_payment_id),
    CONSTRAINT pagos_pkey PRIMARY KEY (id),
    CONSTRAINT pagos_reserva_id_key UNIQUE (reserva_id),
    CONSTRAINT fk_pago_membresia FOREIGN KEY (membresia_id) REFERENCES membresias(id),
    CONSTRAINT fk_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT pagos_reserva_id_fkey FOREIGN KEY (reserva_id) REFERENCES reservas(id)
);

CREATE TABLE suscripciones_mercado_pago (
    id UUID NOT NULL,
    membresia_id UUID NOT NULL,
    pago_inicial_id UUID NOT NULL,
    preapproval_id VARCHAR(100),
    estado VARCHAR(30) NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    checkout_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT suscripciones_mercado_pago_pago_inicial_id_key UNIQUE (pago_inicial_id),
    CONSTRAINT suscripciones_mercado_pago_pkey PRIMARY KEY (id),
    CONSTRAINT suscripciones_mercado_pago_preapproval_id_key UNIQUE (preapproval_id),
    CONSTRAINT suscripciones_mercado_pago_membresia_id_fkey FOREIGN KEY (membresia_id) REFERENCES membresias(id),
    CONSTRAINT suscripciones_mercado_pago_pago_inicial_id_fkey FOREIGN KEY (pago_inicial_id) REFERENCES pagos(id)
);

CREATE TABLE informes (
    id UUID NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL,
    creado_por UUID NOT NULL,
    contenido TEXT NOT NULL,
    CONSTRAINT informes_pkey PRIMARY KEY (id),
    CONSTRAINT informes_creado_por_fkey FOREIGN KEY (creado_por) REFERENCES usuarios(id)
);

CREATE TABLE encuestas (
    id UUID NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descripcion VARCHAR(1000) NOT NULL,
    espacio_id UUID,
    desde DATE NOT NULL,
    hasta DATE NOT NULL,
    activa BOOLEAN NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT encuestas_check CHECK ((desde <= hasta)),
    CONSTRAINT encuestas_pkey PRIMARY KEY (id),
    CONSTRAINT encuestas_espacio_id_fkey FOREIGN KEY (espacio_id) REFERENCES espacios(id)
);

CREATE TABLE preguntas_encuesta (
    id UUID NOT NULL,
    encuesta_id UUID NOT NULL,
    texto VARCHAR(500) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    obligatoria BOOLEAN NOT NULL,
    orden INTEGER NOT NULL,
    CONSTRAINT preguntas_encuesta_tipo_check CHECK ((tipo IN ('CALIFICACION', 'OPCION', 'TEXTO'))),
    CONSTRAINT preguntas_encuesta_encuesta_id_orden_key UNIQUE (encuesta_id, orden),
    CONSTRAINT preguntas_encuesta_pkey PRIMARY KEY (id),
    CONSTRAINT preguntas_encuesta_encuesta_id_fkey FOREIGN KEY (encuesta_id) REFERENCES encuestas(id)
);

CREATE TABLE opciones_pregunta (
    pregunta_id UUID NOT NULL,
    orden INTEGER NOT NULL,
    valor VARCHAR(200) NOT NULL,
    CONSTRAINT opciones_pregunta_pkey PRIMARY KEY (pregunta_id, orden),
    CONSTRAINT opciones_pregunta_pregunta_id_fkey FOREIGN KEY (pregunta_id) REFERENCES preguntas_encuesta(id)
);

CREATE TABLE envios_encuesta (
    id UUID NOT NULL,
    encuesta_id UUID NOT NULL,
    reserva_id UUID NOT NULL,
    enviada_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT envios_encuesta_encuesta_id_reserva_id_key UNIQUE (encuesta_id, reserva_id),
    CONSTRAINT envios_encuesta_pkey PRIMARY KEY (id),
    CONSTRAINT envios_encuesta_encuesta_id_fkey FOREIGN KEY (encuesta_id) REFERENCES encuestas(id),
    CONSTRAINT envios_encuesta_reserva_id_fkey FOREIGN KEY (reserva_id) REFERENCES reservas(id)
);

CREATE TABLE respuestas_encuesta (
    envio_id UUID NOT NULL,
    pregunta_id UUID NOT NULL,
    valor VARCHAR(2000) NOT NULL,
    CONSTRAINT respuestas_encuesta_pkey PRIMARY KEY (envio_id, pregunta_id),
    CONSTRAINT respuestas_encuesta_envio_id_fkey FOREIGN KEY (envio_id) REFERENCES envios_encuesta(id),
    CONSTRAINT respuestas_encuesta_pregunta_id_fkey FOREIGN KEY (pregunta_id) REFERENCES preguntas_encuesta(id)
);

CREATE INDEX ix_bloqueos_espacio_fecha ON bloqueos_espacios USING btree (espacio_id, fecha);

CREATE INDEX ix_cambios_socios_fecha ON cambios_socios USING btree (socio_id, fecha);

CREATE INDEX ix_disponibilidades_espacio ON disponibilidades USING btree (id_espacio);

CREATE INDEX ix_envios_encuesta ON envios_encuesta USING btree (encuesta_id, enviada_en DESC);

CREATE INDEX ix_informes_fecha ON informes USING btree (creado_en DESC);

CREATE INDEX ix_pagos_factura ON pagos USING btree (mercado_pago_factura_id);

CREATE INDEX ix_pagos_usuario_fecha ON pagos USING btree (usuario_id, created_at DESC);

CREATE INDEX ix_reservas_usuario_fecha ON reservas USING btree (usuario_id, fecha DESC);

CREATE INDEX ix_suscripciones_mp_membresia ON suscripciones_mercado_pago USING btree (membresia_id);

CREATE UNIQUE INDEX uk_espacios_nombre_normalizado ON espacios USING btree (lower((nombre)::TEXT));

CREATE UNIQUE INDEX uk_nivel_nombre_normalizado ON nivel_membresias USING btree (lower((nombre)::TEXT));

CREATE UNIQUE INDEX ux_pagos_factura_aplicada ON pagos USING btree (mercado_pago_factura_id) WHERE ((aplicado_en IS NOT NULL) AND (mercado_pago_factura_id IS NOT NULL));

CREATE UNIQUE INDEX ux_pagos_pendiente_membresia ON pagos USING btree (membresia_id, contratacion_id) WHERE ((estado)::TEXT = 'PENDIENTE'::TEXT);

CREATE UNIQUE INDEX ux_pagos_usuario_clave ON pagos USING btree (usuario_id, clave_solicitud);
